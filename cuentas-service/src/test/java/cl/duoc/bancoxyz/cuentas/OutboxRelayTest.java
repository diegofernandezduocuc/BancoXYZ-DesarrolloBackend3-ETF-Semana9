package cl.duoc.bancoxyz.cuentas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxRelayTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, OutboxRelay.TransactionEvent> kafka = mock(KafkaTemplate.class);
    private final OutboxRelay relay = new OutboxRelay(db, kafka, "topic-prueba");
    private final OutboxRelay.TransactionEvent event = new OutboxRelay.TransactionEvent(
            UUID.randomUUID(), "PRUEBA", 1L, BigDecimal.ONE, BigDecimal.TEN, LocalDateTime.now());

    private void pending() {
        when(db.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<OutboxRelay.TransactionEvent>>any()))
                .thenReturn(List.of(event));
    }

    @Test
    void marcaPublicadoSoloDespuesDeConfirmacionKafka() {
        pending();
        when(kafka.send(anyString(), anyString(), eq(event))).thenReturn(CompletableFuture.completedFuture(null));
        relay.publicar();
        var ordered = inOrder(kafka, db);
        ordered.verify(kafka).send("topic-prueba", event.eventId().toString(), event);
        ordered.verify(db).update("UPDATE outbox_eventos SET publicado=1 WHERE id=?", event.eventId().toString());
    }

    @Test
    void falloConservaPendienteYReintentaConElMismoId() {
        pending();
        when(kafka.send(anyString(), anyString(), eq(event)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Fallo de prueba")))
                .thenReturn(CompletableFuture.completedFuture(null));
        relay.publicar();
        verify(db, never()).update(anyString(), any(Object[].class));
        relay.publicar();
        verify(kafka, times(2)).send("topic-prueba", event.eventId().toString(), event);
        verify(db).update("UPDATE outbox_eventos SET publicado=1 WHERE id=?", event.eventId().toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interrupcionSeConservaSinMarcarPublicado() throws Exception {
        pending();
        CompletableFuture<SendResult<String, OutboxRelay.TransactionEvent>> future = mock(CompletableFuture.class);
        when(kafka.send(anyString(), anyString(), eq(event))).thenReturn(future);
        when(future.get(10, TimeUnit.SECONDS)).thenThrow(new InterruptedException("Interrupcion de prueba"));
        try {
            relay.publicar();
            assertTrue(Thread.currentThread().isInterrupted());
            verify(db, never()).update(anyString(), any(Object[].class));
        } finally {
            Thread.interrupted();
        }
    }
}
