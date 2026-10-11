package cl.duoc.bancoxyzbatch.processor;

import cl.duoc.bancoxyzbatch.config.InterestRateProperties;
import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.model.CuentaAnualRaw;
import cl.duoc.bancoxyzbatch.model.InteresRaw;
import cl.duoc.bancoxyzbatch.model.TransaccionRaw;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MonetaryPrecisionTest {
    @Test
    void transaccionesRechazanFraccionesDeCentavoComoDatosInvalidos() {
        var processor = new TransaccionProcessor(new DuplicateRegistry());
        assertThrows(InvalidDataException.class,
                () -> processor.process(new TransaccionRaw("1", "2024-04-09", "10.001", "credito")));
    }

    @Test
    void interesesRechazanFraccionesDeCentavoComoDatosInvalidos() {
        var processor = new InteresProcessor(new DuplicateRegistry(), new InterestRateProperties());
        assertThrows(InvalidDataException.class,
                () -> processor.process(new InteresRaw("101", "Ana", "10.001", "30", "ahorro")));
    }

    @Test
    void anualesRechazanFraccionesDeCentavoComoDatosInvalidos() {
        var processor = new CuentaAnualProcessor(new DuplicateRegistry());
        assertThrows(InvalidDataException.class,
                () -> processor.process(new CuentaAnualRaw("101", "2024-04-09", "deposito", "10.001", "Ingreso")));
    }

    @Test
    void cerosDecimalesAdicionalesNoCambianImportesValidos() {
        var registry = new DuplicateRegistry();
        assertEquals(new BigDecimal("10.00"), new TransaccionProcessor(registry)
                .process(new TransaccionRaw("1", "2024-04-09", "10.000", "credito")).monto());
        assertEquals(new BigDecimal("10.00"), new InteresProcessor(registry, new InterestRateProperties())
                .process(new InteresRaw("101", "Ana", "10.000", "30", "ahorro")).saldoInicial());
        assertEquals(new BigDecimal("10.00"), new CuentaAnualProcessor(registry)
                .process(new CuentaAnualRaw("101", "2024-04-09", "deposito", "10.000", "Ingreso")).monto());
    }
}
