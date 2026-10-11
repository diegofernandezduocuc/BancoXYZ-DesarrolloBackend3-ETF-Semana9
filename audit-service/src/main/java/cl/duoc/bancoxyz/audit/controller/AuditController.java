package cl.duoc.bancoxyz.audit.controller;

import cl.duoc.bancoxyz.audit.event.TransactionEvent;
import cl.duoc.bancoxyz.audit.service.AuditEventStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventStore store;

    public AuditController(AuditEventStore store) {
        this.store = store;
    }

    @GetMapping("/eventos")
    public List<TransactionEvent> listarEventos() {
        return store.findAll();
    }
}
