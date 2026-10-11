package cl.duoc.bancoxyzbatch.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateNormalizerTest {
    @Test
    void aceptaFormatosLegacyPermitidos() {
        assertEquals(LocalDate.of(2024, 3, 18), DateNormalizer.parse("2024/03/18"));
        assertEquals(LocalDate.of(2024, 3, 8), DateNormalizer.parse("08-03-2024"));
        assertEquals(LocalDate.of(2024, 3, 24), DateNormalizer.parse("24/03/2024"));
        assertEquals(LocalDate.of(2024, 6, 30), DateNormalizer.parse("2024-06-30"));
    }

    @Test
    void rechazaFechaImposible() {
        assertThrows(InvalidDataException.class, () -> DateNormalizer.parse("2024-13-01"));
    }
}
