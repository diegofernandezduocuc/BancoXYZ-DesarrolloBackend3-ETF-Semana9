package cl.duoc.bancoxyzbatch.processor;

import cl.duoc.bancoxyzbatch.config.InterestRateProperties;
import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.model.InteresCalculado;
import cl.duoc.bancoxyzbatch.model.InteresRaw;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import cl.duoc.bancoxyzbatch.support.HashUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class InteresProcessor implements ItemProcessor<InteresRaw, InteresCalculado> {
    public static final String JOB_NAME = "interesesJob";
    private static final Set<String> ALLOWED_TYPES = Set.of("ahorro", "prestamo", "hipoteca");

    private final DuplicateRegistry duplicateRegistry;
    private final InterestRateProperties rates;

    public InteresProcessor(DuplicateRegistry duplicateRegistry, InterestRateProperties rates) {
        this.duplicateRegistry = duplicateRegistry;
        this.rates = rates;
    }

    @Override
    public InteresCalculado process(InteresRaw item) {
        String canonical = String.join("|", safe(item.cuentaId()), safe(item.nombre()), safe(item.saldo()),
                safe(item.edad()), safe(item.tipo()));
        if (!duplicateRegistry.firstOccurrence(JOB_NAME, canonical)) {
            throw new InvalidDataException("Registro duplicado");
        }

        long cuentaId = parsePositiveLong(item.cuentaId(), "cuenta_id");
        String nombre = safe(item.nombre());
        if (nombre.isBlank() || nombre.equalsIgnoreCase("unknown")) {
            throw new InvalidDataException("Nombre de titular inválido");
        }
        BigDecimal saldo = parsePositiveDecimal(item.saldo(), "saldo");
        int edad = parseAge(item.edad());
        String tipo = safe(item.tipo()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(tipo)) {
            throw new InvalidDataException("Tipo de cuenta inválido: " + item.tipo());
        }

        BigDecimal tasa = rates.rateFor(tipo);
        BigDecimal interes = saldo.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoFinal = saldo.add(interes).setScale(2, RoundingMode.HALF_UP);

        return new InteresCalculado(HashUtils.sha256(canonical), cuentaId, nombre, saldo, edad,
                tipo, tasa, interes, saldoFinal);
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

    private int parseAge(String raw) {
        try {
            int value = Integer.parseInt(safe(raw));
            if (value < 18 || value > 99) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidDataException("Edad fuera de rango: " + raw);
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
