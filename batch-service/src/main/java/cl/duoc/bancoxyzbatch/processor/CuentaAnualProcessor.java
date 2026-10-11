package cl.duoc.bancoxyzbatch.processor;

import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.model.CuentaAnualRaw;
import cl.duoc.bancoxyzbatch.model.MovimientoAnual;
import cl.duoc.bancoxyzbatch.support.DateNormalizer;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import cl.duoc.bancoxyzbatch.support.HashUtils;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class CuentaAnualProcessor implements ItemProcessor<CuentaAnualRaw, MovimientoAnual> {
    public static final String JOB_NAME = "estadosAnualesJob";
    private static final Set<String> ALLOWED_TYPES = Set.of("deposito", "retiro", "compra", "pago");

    private final DuplicateRegistry duplicateRegistry;

    public CuentaAnualProcessor(DuplicateRegistry duplicateRegistry) {
        this.duplicateRegistry = duplicateRegistry;
    }

    @Override
    public MovimientoAnual process(CuentaAnualRaw item) {
        String canonical = String.join("|", safe(item.cuentaId()), safe(item.fecha()), safe(item.transaccion()),
                safe(item.monto()), safe(item.descripcion()));
        if (!duplicateRegistry.firstOccurrence(JOB_NAME, canonical)) {
            throw new InvalidDataException("Registro duplicado");
        }

        long cuentaId = parsePositiveLong(item.cuentaId(), "cuenta_id");
        var fecha = DateNormalizer.parse(item.fecha());
        String tipo = normalizeType(item.transaccion());
        if (!ALLOWED_TYPES.contains(tipo)) {
            throw new InvalidDataException("Tipo de movimiento inválido: " + item.transaccion());
        }
        BigDecimal monto = parsePositiveDecimal(item.monto(), "monto");
        String descripcion = safe(item.descripcion());
        if (descripcion.isBlank()) {
            throw new InvalidDataException("Descripción faltante");
        }

        BigDecimal montoFirmado = tipo.equals("deposito") ? monto : monto.negate();
        return new MovimientoAnual(HashUtils.sha256(canonical), cuentaId, fecha, tipo,
                monto, descripcion, montoFirmado);
    }

    private String normalizeType(String value) {
        String normalized = Normalizer.normalize(safe(value).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized;
    }

    private long parsePositiveLong(String raw, String field) {
        try {
            long value = Long.parseLong(safe(raw));
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidDataException(field + " inválido: " + raw);
        }
    }

    private BigDecimal parsePositiveDecimal(String raw, String field) {
        try {
            BigDecimal value = new BigDecimal(safe(raw));
            if (value.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            return value.setScale(2);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new InvalidDataException(field + " inválido: " + raw);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
