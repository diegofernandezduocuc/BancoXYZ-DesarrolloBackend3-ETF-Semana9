package cl.duoc.bancoxyz.audit.service;

import cl.duoc.bancoxyz.audit.event.TransactionEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditEventStoreTest {

    @Test
    void evitaDuplicarElMismoEvento() {
        AuditEventStore store = new AuditEventStore();
        UUID id = UUID.randomUUID();
        TransactionEvent event = new TransactionEvent(
                id, "RETIRO_REALIZADO", 106L, new BigDecimal("1000"),
                new BigDecimal("11000"), LocalDateTime.now());

        store.save(event);
        store.save(event);

        assertEquals(1, store.findAll().size());
    }
}
