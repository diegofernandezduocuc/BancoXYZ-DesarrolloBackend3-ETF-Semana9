package cl.duoc.bancoxyz.atm.service;

import cl.duoc.bancoxyz.atm.client.BackendClient;
import cl.duoc.bancoxyz.atm.client.BackendCuenta;
import cl.duoc.bancoxyz.atm.client.BackendRetiroResponse;
import cl.duoc.bancoxyz.atm.model.AtmRetiroResponse;
import cl.duoc.bancoxyz.atm.model.AtmSaldoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AtmService {

    private final BackendClient backendClient;

    public AtmService(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    public AtmSaldoResponse consultarSaldo(Long id) {
        BackendCuenta cuenta = backendClient.getCuenta(id);
        return new AtmSaldoResponse(cuenta.id(), cuenta.saldo());
    }

    public AtmRetiroResponse retirar(Long id, BigDecimal amount) {
        BackendRetiroResponse response = backendClient.retirar(id, amount);
        return new AtmRetiroResponse(
                response.cuentaId(),
                response.monto(),
                response.saldoActual(),
                response.fechaHora(),
                response.estado()
        );
    }
}
