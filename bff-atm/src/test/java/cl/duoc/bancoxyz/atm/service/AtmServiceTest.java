package cl.duoc.bancoxyz.atm.service;

import cl.duoc.bancoxyz.atm.client.BackendClient;
import cl.duoc.bancoxyz.atm.client.BackendCuenta;
import cl.duoc.bancoxyz.atm.client.BackendRetiroResponse;
import cl.duoc.bancoxyz.atm.model.AtmRetiroResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AtmServiceTest {

    @Test
    void consultaSoloElSaldoNecesarioParaElCajero() {
        BackendClient client = mock(BackendClient.class);
        when(client.getCuenta(106L)).thenReturn(new BackendCuenta(106L, "Jane Smith", new BigDecimal("12000"), 40, "prestamo"));

        assertEquals(new BigDecimal("12000"), new AtmService(client).consultarSaldo(106L).saldoDisponible());
    }

    @Test
    void adaptaLaRespuestaDeRetiroParaElCanalAtm() {
        BackendClient client = mock(BackendClient.class);
        LocalDateTime now = LocalDateTime.now();
        when(client.retirar(106L, new BigDecimal("1000"))).thenReturn(
                new BackendRetiroResponse(106L, new BigDecimal("1000"), new BigDecimal("12000"), new BigDecimal("11000"), now, "APROBADO")
        );

        AtmRetiroResponse response = new AtmService(client).retirar(106L, new BigDecimal("1000"));

        assertEquals(new BigDecimal("11000"), response.saldoDisponible());
        assertEquals("APROBADO", response.estado());
    }
}
