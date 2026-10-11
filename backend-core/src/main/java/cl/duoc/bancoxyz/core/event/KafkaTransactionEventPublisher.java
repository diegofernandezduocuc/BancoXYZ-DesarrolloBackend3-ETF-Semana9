package cl.duoc.bancoxyz.core.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final String topic;

    public KafkaTransactionEventPublisher(
            KafkaTemplate<String, TransactionEvent> kafkaTemplate,
            @Value("${bancoxyz.kafka.topic-transacciones:bancoxyz.transacciones}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(TransactionEvent event) {
        kafkaTemplate.send(topic, event.eventId().toString(), event);
    }
}
