package cl.duoc.bancoxyz.core.event;

public interface TransactionEventPublisher {
    void publish(TransactionEvent event);
}
