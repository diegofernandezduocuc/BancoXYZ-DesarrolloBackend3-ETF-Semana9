package cl.duoc.bancoxyzbatch.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.model.TransaccionRaw;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransaccionProcessorTest {
    private TransaccionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransaccionProcessor(new DuplicateRegistry());
    }

    @Test
    void normalizaTransaccionValida() {
        var result = processor.process(new TransaccionRaw("1", "03-04-2024", "1200", "credito"));
        assertEquals(new BigDecimal("1200.00"), result.monto());
        assertEquals(new BigDecimal("1200.00"), result.montoFirmado());
    }

    @Test
    void rechazaMontoNoPositivo() {
        assertThrows(InvalidDataException.class,
            () -> processor.process(new TransaccionRaw("2", "2024-04-09", "-200", "debito")));
    }

    @Test
    void rechazaTipoInvalido() {
        assertThrows(InvalidDataException.class,
            () -> processor.process(new TransaccionRaw("3", "2024-04-09", "800", "invalid")));
    }
}
