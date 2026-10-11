package cl.duoc.bancoxyzbatch.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.duoc.bancoxyzbatch.config.InterestRateProperties;
import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.model.InteresRaw;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InteresProcessorTest {
    private InteresProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new InteresProcessor(new DuplicateRegistry(), new InterestRateProperties());
    }

    @Test
    void calculaInteresDeAhorro() {
        var result = processor.process(new InteresRaw("101", "Ana Perez", "10000", "30", "ahorro"));
        assertEquals(new BigDecimal("50.00"), result.interes());
        assertEquals(new BigDecimal("10050.00"), result.saldoFinal());
    }

    @Test
    void rechazaEdadFueraDeRango() {
        assertThrows(InvalidDataException.class,
            () -> processor.process(new InteresRaw("101", "Ana Perez", "10000", "150", "ahorro")));
    }

    @Test
    void rechazaTipoNoReconocido() {
        assertThrows(InvalidDataException.class,
            () -> processor.process(new InteresRaw("102", "Ana Perez", "10000", "30", "-1")));
    }
}
