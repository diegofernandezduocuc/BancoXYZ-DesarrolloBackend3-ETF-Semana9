package cl.duoc.bancoxyz.cuentas;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class CuentaRepository {
    private final JdbcTemplate db;
    public CuentaRepository(JdbcTemplate db) { this.db = db; }

    public record Cuenta(Long id, String titular, BigDecimal saldo, Integer edad, String tipo) {}
    public record Movimiento(Long cuentaId, LocalDate fecha, String tipo, BigDecimal monto, String descripcion) {}
    public record Resultado(Long cuentaId, BigDecimal monto, BigDecimal saldoAnterior, BigDecimal saldoActual,
                            LocalDateTime fechaHora, String estado) {}
    public record Transferencia(Long origenId, Long destinoId, BigDecimal monto, String estado) {}

    public List<Cuenta> listar() {
        return db.query("SELECT id, titular, saldo, edad, tipo FROM cuentas WHERE activa=1 ORDER BY id",
                (rs, n) -> new Cuenta(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getInt(4), rs.getString(5)));
    }
    public Cuenta obtener(long id) {
        return db.query("SELECT id,titular,saldo,edad,tipo FROM cuentas WHERE id=? AND activa=1",
                (rs,n)->new Cuenta(rs.getLong(1),rs.getString(2),rs.getBigDecimal(3),rs.getInt(4),rs.getString(5)),id)
                .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cuenta no encontrada"));
    }
    public List<Movimiento> movimientos(long id, int limit) {
        obtener(id);
        return db.query("SELECT cuenta_id,fecha,tipo,monto,descripcion FROM movimientos WHERE cuenta_id=? ORDER BY id DESC LIMIT ?",
                (rs,n)->new Movimiento(rs.getLong(1),rs.getDate(2).toLocalDate(),rs.getString(3),rs.getBigDecimal(4),rs.getString(5)),
                id,Math.max(1,Math.min(50,limit)));
    }
    public long apertura(String nombre, int edad, String tipo, BigDecimal saldo) {
        if (nombre==null || nombre.isBlank() || edad<18 || edad>99 || saldo==null || saldo.signum()<0 ||
            !List.of("ahorro","prestamo","hipoteca").contains(tipo))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Datos de cuenta inválidos");
        validarPrecision(saldo);
        org.springframework.jdbc.support.KeyHolder key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        db.update(connection -> {
            var ps=connection.prepareStatement("INSERT INTO cuentas(titular,saldo,edad,tipo,activa) VALUES(?,?,?,?,1)",java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,nombre);ps.setBigDecimal(2,saldo);ps.setInt(3,edad);ps.setString(4,tipo);return ps;
        },key);
        return key.getKey().longValue();
    }
    @Transactional
    public void cerrar(long id) {
        BigDecimal saldo=saldoBloqueado(id);
        if (saldo.signum()!=0) throw new ResponseStatusException(HttpStatus.CONFLICT,"La cuenta debe quedar sin saldo para cerrarse");
        db.update("UPDATE cuentas SET activa=0 WHERE id=?",id);
    }
    private BigDecimal saldoBloqueado(long id) {
        return db.query("SELECT saldo FROM cuentas WHERE id=? AND activa=1 FOR UPDATE",(rs,n)->rs.getBigDecimal(1),id)
                .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cuenta no encontrada"));
    }
    private void validarMonto(BigDecimal monto) {
        if (monto==null || monto.signum()<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Monto debe ser mayor que cero");
        validarPrecision(monto);
    }
    private void validarPrecision(BigDecimal monto) {
        try {
            monto.setScale(2);
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El importe debe ser representable con dos decimales");
        }
    }
    @Transactional
    public Resultado movimiento(long id,BigDecimal monto,boolean debito,String tipo) {
        validarMonto(monto);
        BigDecimal previo=saldoBloqueado(id);
        if (debito && previo.compareTo(monto)<0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Saldo insuficiente");
        BigDecimal nuevo=debito?previo.subtract(monto):previo.add(monto);
        db.update("UPDATE cuentas SET saldo=? WHERE id=?",nuevo,id);
        agregarMovimiento(id,monto,tipo,"Operación bancaria registrada");
        agregarEvento(id,monto,nuevo,tipo.toUpperCase()+"_REALIZADO");
        return new Resultado(id,monto,previo,nuevo,LocalDateTime.now(),"APROBADO");
    }
    @Transactional
    public Transferencia transferir(long origen,long destino,BigDecimal monto) {
        validarMonto(monto);
        if (origen==destino) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El destino debe ser distinto");
        // Bloqueo ordenado evita interbloqueos entre transferencias cruzadas.
        long menor=Math.min(origen,destino),mayor=Math.max(origen,destino);
        BigDecimal saldoMenor=saldoBloqueado(menor);
        BigDecimal saldoMayor=saldoBloqueado(mayor);
        BigDecimal saldoOrigen=origen==menor?saldoMenor:saldoMayor;
        BigDecimal saldoDestino=destino==menor?saldoMenor:saldoMayor;
        if (saldoOrigen.compareTo(monto)<0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Saldo insuficiente");
        BigDecimal origenNuevo=saldoOrigen.subtract(monto), destinoNuevo=saldoDestino.add(monto);
        db.update("UPDATE cuentas SET saldo=? WHERE id=?",origenNuevo,origen);
        db.update("UPDATE cuentas SET saldo=? WHERE id=?",destinoNuevo,destino);
        agregarMovimiento(origen,monto,"transferencia","Transferencia a cuenta "+destino);
        agregarMovimiento(destino,monto,"deposito","Transferencia recibida de cuenta "+origen);
        agregarEvento(origen,monto,origenNuevo,"TRANSFERENCIA_COMPLETADA");
        return new Transferencia(origen,destino,monto,"APROBADO");
    }
    private void agregarMovimiento(long id, BigDecimal monto,String tipo,String descripcion) {
        db.update("INSERT INTO movimientos(cuenta_id,fecha,tipo,monto,descripcion) VALUES(?,?,?,?,?)",
                id,java.sql.Date.valueOf(LocalDate.now()),tipo,monto,descripcion);
    }
    private void agregarEvento(long id,BigDecimal monto,BigDecimal nuevo,String tipo) {
        UUID uuid=UUID.randomUUID();
        db.update("INSERT INTO outbox_eventos(id,tipo,cuenta_id,monto,saldo_resultante,creado) VALUES(?,?,?,?,?,NOW())",
                uuid.toString(),tipo,id,monto,nuevo);
    }
}
