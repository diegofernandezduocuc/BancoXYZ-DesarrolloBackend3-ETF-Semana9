package cl.duoc.bancoxyz.mobile.service;

import cl.duoc.bancoxyz.mobile.client.BackendClient;
import cl.duoc.bancoxyz.mobile.client.BackendCuenta;
import cl.duoc.bancoxyz.mobile.model.MobileCuentaResponse;
import cl.duoc.bancoxyz.mobile.model.MobileMovimientoResponse;
import cl.duoc.bancoxyz.mobile.model.MobileSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MobileSummaryService {

    private final BackendClient backendClient;

    public MobileSummaryService(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    public List<MobileCuentaResponse> listarCuentas() {
        return backendClient.getCuentas().stream()
                .map(cuenta -> new MobileCuentaResponse(cuenta.id(), cuenta.titular(), cuenta.saldo()))
                .toList();
    }

    public MobileSummaryResponse obtenerResumen(Long id) {
        BackendCuenta cuenta = backendClient.getCuenta(id);
        List<MobileMovimientoResponse> movimientos = backendClient.getMovimientos(id, 3).stream()
                .map(movimiento -> new MobileMovimientoResponse(
                        movimiento.fecha(), movimiento.tipo(), movimiento.monto()))
                .toList();

        return new MobileSummaryResponse(
                cuenta.id(),
                cuenta.titular(),
                cuenta.saldo(),
                movimientos
        );
    }
}
