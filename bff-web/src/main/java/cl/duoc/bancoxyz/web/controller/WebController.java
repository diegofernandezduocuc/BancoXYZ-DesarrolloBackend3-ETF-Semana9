package cl.duoc.bancoxyz.web.controller;

import cl.duoc.bancoxyz.web.model.WebCuentaResponse;
import cl.duoc.bancoxyz.web.model.WebDashboardResponse;
import cl.duoc.bancoxyz.web.service.WebAggregationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/web")
public class WebController {

    private final WebAggregationService service;

    public WebController(WebAggregationService service) {
        this.service = service;
    }

    @GetMapping("/cuentas")
    public List<WebCuentaResponse> listarCuentas() {
        return service.listarCuentas();
    }

    @GetMapping("/cuentas/{id}/dashboard")
    public WebDashboardResponse obtenerDashboard(@PathVariable Long id) {
        return service.obtenerDashboard(id);
    }
}
