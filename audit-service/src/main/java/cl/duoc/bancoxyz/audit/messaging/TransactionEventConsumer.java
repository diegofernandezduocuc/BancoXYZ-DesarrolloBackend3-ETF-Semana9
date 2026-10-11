package cl.duoc.bancoxyz.audit.messaging;

import cl.duoc.bancoxyz.audit.event.TransactionEvent;
import cl.duoc.bancoxyz.audit.service.AuditEventStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    private final AuditEventStore store;

    public TransactionEventConsumer(AuditEventStore store) {
        this.store = store;
    }

    @KafkaListener(topics = "${bancoxyz.kafka.topic-transacciones:bancoxyz.transacciones}")
    public void consume(TransactionEvent event) {
        store.save(event);
    }
}
