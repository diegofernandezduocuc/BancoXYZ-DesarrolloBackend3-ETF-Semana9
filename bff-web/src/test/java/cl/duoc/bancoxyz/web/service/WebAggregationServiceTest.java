package cl.duoc.bancoxyz.web.service;

import cl.duoc.bancoxyz.web.client.BackendClient;
import cl.duoc.bancoxyz.web.client.BackendCuenta;
import cl.duoc.bancoxyz.web.client.BackendMovimiento;
import cl.duoc.bancoxyz.web.client.BackendResumenTransacciones;
import cl.duoc.bancoxyz.web.model.WebDashboardResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebAggregationServiceTest {

    @Test
    void agregaInformacionDeTresServiciosBackend() {
        BackendClient client = mock(BackendClient.class);
        when(client.getCuenta(106L)).thenReturn(new BackendCuenta(106L, "Jane Smith", new BigDecimal("12000"), 40, "prestamo"));
        when(client.getMovimientos(106L, 10)).thenReturn(List.of(
                new BackendMovimiento(106L, LocalDate.of(2024, 6, 1), "deposito", new BigDecimal("1000"), "Ingreso")
        ));
        when(client.getResumenTransacciones()).thenReturn(new BackendResumenTransacciones(
                401, 200, 201, new BigDecimal("259000"), new BigDecimal("268000"), new BigDecimal("-9000")));

        WebDashboardResponse response = new WebAggregationService(client).obtenerDashboard(106L);

        assertEquals(106L, response.cuentaId());
        assertEquals(1, response.totalMovimientosIncluidos());
        assertEquals(401, response.resumenBanco().registrosValidos());
    }
}
