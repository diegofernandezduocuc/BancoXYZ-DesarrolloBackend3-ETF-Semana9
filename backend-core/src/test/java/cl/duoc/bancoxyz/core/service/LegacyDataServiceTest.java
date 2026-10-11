package cl.duoc.bancoxyz.core.service;

import cl.duoc.bancoxyz.core.event.TransactionEventPublisher;
import cl.duoc.bancoxyz.core.exception.BusinessRuleException;
import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.RetiroResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LegacyDataServiceTest {

    private LegacyDataService service;
    private TransactionEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = mock(TransactionEventPublisher.class);
        service = new LegacyDataService(publisher);
        service.loadData();
    }

    @Test
    void cargaCuentasValidasDesdeDatosLegacy() {
        assertFalse(service.getCuentas().isEmpty());
        Cuenta cuenta = service.getCuenta(106L);
        assertEquals("Jane Smith", cuenta.titular());
        assertEquals(new BigDecimal("12000"), cuenta.saldo());
    }

    @Test
    void obtieneMovimientosLimitados() {
        assertTrue(service.getMovimientos(106L, 3).size() <= 3);
    }

    @Test
    void generaResumenDeTransaccionesValidas() {
        assertTrue(service.getTransactionSummary().registrosValidos() > 0);
        assertTrue(service.getTransactionSummary().creditos() > 0);
        assertTrue(service.getTransactionSummary().debitos() > 0);
    }

    @Test
    void retiroActualizaSaldo() {
        RetiroResponse response = service.retirar(106L, new BigDecimal("1000"));
        assertEquals(new BigDecimal("11000"), response.saldoActual());
        assertEquals(new BigDecimal("11000"), service.getCuenta(106L).saldo());
        verify(publisher).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retiroRechazaSaldoInsuficiente() {
        assertThrows(BusinessRuleException.class,
                () -> service.retirar(106L, new BigDecimal("999999")));
    }
}
