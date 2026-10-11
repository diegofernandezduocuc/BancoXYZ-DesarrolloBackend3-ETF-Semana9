package cl.duoc.bancoxyz.web.service;

import cl.duoc.bancoxyz.web.client.BackendClient;
import cl.duoc.bancoxyz.web.client.BackendCuenta;
import cl.duoc.bancoxyz.web.client.BackendMovimiento;
import cl.duoc.bancoxyz.web.client.BackendResumenTransacciones;
import cl.duoc.bancoxyz.web.model.WebCuentaResponse;
import cl.duoc.bancoxyz.web.model.WebDashboardResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebAggregationService {

    private final BackendClient backendClient;

    public WebAggregationService(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    public List<WebCuentaResponse> listarCuentas() {
        return backendClient.getCuentas().stream()
                .map(cuenta -> new WebCuentaResponse(
                        cuenta.id(), cuenta.titular(), cuenta.saldo(), cuenta.edad(), cuenta.tipo()))
                .toList();
    }

    public WebDashboardResponse obtenerDashboard(Long id) {
        BackendCuenta cuenta = backendClient.getCuenta(id);
        List<BackendMovimiento> movimientos = backendClient.getMovimientos(id, 10);
        BackendResumenTransacciones resumen = backendClient.getResumenTransacciones();

        return new WebDashboardResponse(
                cuenta.id(),
                cuenta.titular(),
                cuenta.edad(),
                cuenta.tipo(),
                cuenta.saldo(),
                movimientos,
                resumen,
                movimientos.size()
        );
    }
}
