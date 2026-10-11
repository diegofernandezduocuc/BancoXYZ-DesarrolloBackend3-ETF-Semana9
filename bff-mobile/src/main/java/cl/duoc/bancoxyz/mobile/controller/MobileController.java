package cl.duoc.bancoxyz.mobile.controller;

import cl.duoc.bancoxyz.mobile.model.MobileCuentaResponse;
import cl.duoc.bancoxyz.mobile.model.MobileSummaryResponse;
import cl.duoc.bancoxyz.mobile.service.MobileSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile")
public class MobileController {

    private final MobileSummaryService service;

    public MobileController(MobileSummaryService service) {
        this.service = service;
    }

    @GetMapping("/cuentas")
    public List<MobileCuentaResponse> listarCuentas() {
        return service.listarCuentas();
    }

    @GetMapping("/cuentas/{id}/resumen")
    public MobileSummaryResponse obtenerResumen(@PathVariable Long id) {
        return service.obtenerResumen(id);
    }
}
