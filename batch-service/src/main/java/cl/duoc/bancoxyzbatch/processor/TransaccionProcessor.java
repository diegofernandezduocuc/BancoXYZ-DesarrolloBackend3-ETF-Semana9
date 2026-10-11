package cl.duoc.bancoxyzbatch.processor;

import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.model.TransaccionProcesada;
import cl.duoc.bancoxyzbatch.model.TransaccionRaw;
import cl.duoc.bancoxyzbatch.support.DateNormalizer;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class TransaccionProcessor implements ItemProcessor<TransaccionRaw, TransaccionProcesada> {
    public static final String JOB_NAME = "transaccionesJob";
    private final DuplicateRegistry duplicateRegistry;

    public TransaccionProcessor(DuplicateRegistry duplicateRegistry) {
        this.duplicateRegistry = duplicateRegistry;
    }

    @Override
    public TransaccionProcesada process(TransaccionRaw item) {
        String fingerprint = String.join("|", safe(item.id()), safe(item.fecha()), safe(item.monto()), safe(item.tipo()));
        if (!duplicateRegistry.firstOccurrence(JOB_NAME, fingerprint)) {
            throw new InvalidDataException("Registro duplicado");
        }

        long id = parsePositiveLong(item.id(), "ID");
        var fecha = DateNormalizer.parse(item.fecha());
        BigDecimal monto = parsePositiveDecimal(item.monto(), "monto");
        String tipo = safe(item.tipo()).toLowerCase(Locale.ROOT);
        if (!tipo.equals("credito") && !tipo.equals("debito")) {
            throw new InvalidDataException("Tipo de transacción inválido: " + item.tipo());
        }

        BigDecimal montoFirmado = tipo.equals("credito") ? monto : monto.negate();
        return new TransaccionProcesada(id, fecha, monto, tipo, montoFirmado);
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
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
            return value.setScale(2);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new InvalidDataException(field + " inválido: " + raw);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
