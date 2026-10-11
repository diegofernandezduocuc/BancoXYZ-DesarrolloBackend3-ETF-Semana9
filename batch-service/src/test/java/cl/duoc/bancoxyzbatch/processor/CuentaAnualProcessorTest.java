package cl.duoc.bancoxyzbatch.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.model.CuentaAnualRaw;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CuentaAnualProcessorTest {
    private CuentaAnualProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CuentaAnualProcessor(new DuplicateRegistry());
    }

    @Test
    void normalizaDepositoConAcento() {
        var result = processor.process(new CuentaAnualRaw("101", "2024/03/18", "depósito", "3000", "Ingreso mensual"));
        assertEquals("deposito", result.transaccion());
        assertEquals(new BigDecimal("3000.00"), result.montoFirmado());
    }

    @Test
    void retiroTieneImpactoNegativo() {
        var result = processor.process(new CuentaAnualRaw("102", "08-03-2024", "retiro", "1000", "Retiro parcial"));
        assertEquals(new BigDecimal("-1000.00"), result.montoFirmado());
    }

    @Test
    void rechazaDescripcionFaltante() {
        assertThrows(InvalidDataException.class,
            () -> processor.process(new CuentaAnualRaw("103", "08-03-2024", "retiro", "1000", "")));
    }
}
