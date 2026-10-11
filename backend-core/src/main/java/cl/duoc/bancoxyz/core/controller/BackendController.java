package cl.duoc.bancoxyz.core.controller;

import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.Movimiento;
import cl.duoc.bancoxyz.core.model.ResumenTransacciones;
import cl.duoc.bancoxyz.core.model.RetiroRequest;
import cl.duoc.bancoxyz.core.model.RetiroResponse;
import cl.duoc.bancoxyz.core.service.LegacyDataService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/backend")
public class BackendController {

    private final LegacyDataService service;

    public BackendController(LegacyDataService service) {
        this.service = service;
    }

    @GetMapping("/cuentas")
    public List<Cuenta> listarCuentas() {
        return service.getCuentas();
    }

    @GetMapping("/cuentas/{id}")
    public Cuenta obtenerCuenta(@PathVariable Long id) {
        return service.getCuenta(id);
    }

    @GetMapping("/cuentas/{id}/movimientos")
    public List<Movimiento> obtenerMovimientos(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        return service.getMovimientos(id, limit);
    }

    @GetMapping("/resumen/transacciones")
    public ResumenTransacciones obtenerResumenTransacciones() {
        return service.getTransactionSummary();
    }

    @PostMapping("/cuentas/{id}/retiros")
    public ResponseEntity<RetiroResponse> retirar(
            @PathVariable Long id,
            @Valid @RequestBody RetiroRequest request) {
        return ResponseEntity.ok(service.retirar(id, request.monto()));
    }
}
