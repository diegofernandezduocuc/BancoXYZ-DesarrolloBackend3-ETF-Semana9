package cl.duoc.bancoxyzbatch.support;

import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

public final class DateNormalizer {
    private static final List<DateTimeFormatter> FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT)
    );

    private DateNormalizer() { }

    public static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("Fecha vacía");
        }
        String normalized = value.trim();
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Se prueba el siguiente formato legacy permitido.
            }
        }
        throw new InvalidDataException("Fecha inválida: " + value);
    }
}
