package cl.duoc.bancoxyz.mobile.service;

import cl.duoc.bancoxyz.mobile.client.BackendClient;
import cl.duoc.bancoxyz.mobile.client.BackendCuenta;
import cl.duoc.bancoxyz.mobile.client.BackendMovimiento;
import cl.duoc.bancoxyz.mobile.model.MobileSummaryResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MobileSummaryServiceTest {

    @Test
    void entregaRespuestaLigeraConTresMovimientosComoMaximo() {
        BackendClient client = mock(BackendClient.class);
        when(client.getCuenta(106L)).thenReturn(new BackendCuenta(106L, "Jane Smith", new BigDecimal("12000"), 40, "prestamo"));
        when(client.getMovimientos(106L, 3)).thenReturn(List.of(
                new BackendMovimiento(106L, LocalDate.of(2024, 6, 1), "deposito", new BigDecimal("1000"), "Ingreso"),
                new BackendMovimiento(106L, LocalDate.of(2024, 5, 1), "compra", new BigDecimal("500"), "Compra")
        ));

        MobileSummaryResponse response = new MobileSummaryService(client).obtenerResumen(106L);

        assertEquals(106L, response.cuentaId());
        assertEquals(2, response.ultimosMovimientos().size());
    }
}
