package cl.duoc.bancoxyz.web.model;

import cl.duoc.bancoxyz.web.client.BackendMovimiento;
import cl.duoc.bancoxyz.web.client.BackendResumenTransacciones;

import java.math.BigDecimal;
import java.util.List;

public record WebDashboardResponse(
        Long cuentaId,
        String titular,
        Integer edad,
        String tipoCuenta,
        BigDecimal saldoDisponible,
        List<BackendMovimiento> movimientosRecientes,
        BackendResumenTransacciones resumenBanco,
        int totalMovimientosIncluidos
) {
}
