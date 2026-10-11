package cl.duoc.bancoxyz.atm.controller;

import cl.duoc.bancoxyz.atm.model.AtmRetiroRequest;
import cl.duoc.bancoxyz.atm.model.AtmRetiroResponse;
import cl.duoc.bancoxyz.atm.model.AtmSaldoResponse;
import cl.duoc.bancoxyz.atm.service.AtmService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atm")
public class AtmController {

    private final AtmService service;

    public AtmController(AtmService service) {
        this.service = service;
    }

    @GetMapping("/cuentas/{id}/saldo")
    public AtmSaldoResponse consultarSaldo(@PathVariable Long id) {
        return service.consultarSaldo(id);
    }

    @PostMapping("/cuentas/{id}/retiros")
    public AtmRetiroResponse retirar(
            @PathVariable Long id,
            @Valid @RequestBody AtmRetiroRequest request) {
        return service.retirar(id, request.monto());
    }
}
