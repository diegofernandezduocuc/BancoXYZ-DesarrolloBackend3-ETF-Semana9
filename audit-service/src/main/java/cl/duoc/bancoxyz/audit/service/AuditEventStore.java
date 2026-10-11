package cl.duoc.bancoxyz.audit.service;

import cl.duoc.bancoxyz.audit.event.TransactionEvent;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuditEventStore {

    private final Map<UUID, TransactionEvent> events = new ConcurrentHashMap<>();
    private JdbcTemplate db;

    @Autowired(required=false)
    public void setJdbcTemplate(JdbcTemplate db) { this.db = db; }

    public void save(TransactionEvent event) {
        if (db != null) {
            db.update("INSERT IGNORE INTO auditoria_eventos(id,tipo,cuenta_id,monto,saldo_resultante,fecha_hora) VALUES(?,?,?,?,?,?)",
                    event.eventId().toString(),event.tipo(),event.cuentaId(),event.monto(),event.saldoResultante(),
                    java.sql.Timestamp.valueOf(event.fechaHora()));
        } else {
            events.putIfAbsent(event.eventId(), event);
        }
    }

    public List<TransactionEvent> findAll() {
        if (db != null) {
            return db.query("SELECT id,tipo,cuenta_id,monto,saldo_resultante,fecha_hora FROM auditoria_eventos ORDER BY fecha_hora DESC LIMIT 500",
                (rs,n)->new TransactionEvent(UUID.fromString(rs.getString(1)),rs.getString(2),rs.getLong(3),
                    rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getTimestamp(6).toLocalDateTime()));
        }
        return events.values().stream().sorted(Comparator.comparing(TransactionEvent::fechaHora).reversed()).toList();
    }
}
