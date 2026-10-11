package cl.duoc.bancoxyz.cuentas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@EnableScheduling
@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    public record TransactionEvent(UUID eventId,String tipo,Long cuentaId,BigDecimal monto,
                                   BigDecimal saldoResultante,LocalDateTime fechaHora) {}
    private final JdbcTemplate db;
    private final KafkaTemplate<String,TransactionEvent> kafka;
    private final String topic;
    public OutboxRelay(JdbcTemplate db,KafkaTemplate<String,TransactionEvent> kafka,
                      @Value("${bancoxyz.kafka.topic-transacciones:bancoxyz.transacciones}") String topic) {
        this.db=db;this.kafka=kafka;this.topic=topic;
    }
    @Scheduled(fixedDelayString="${bancoxyz.outbox.intervalo-ms:5000}",initialDelay=7000)
    public void publicar() {
        List<TransactionEvent> events=db.query("SELECT id,tipo,cuenta_id,monto,saldo_resultante,creado FROM outbox_eventos WHERE publicado=0 ORDER BY creado,id LIMIT 30",
            (rs,n)->new TransactionEvent(UUID.fromString(rs.getString(1)),rs.getString(2),rs.getLong(3),rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getTimestamp(6).toLocalDateTime()));
        for (TransactionEvent event:events) {
            try {
                kafka.send(topic,event.eventId().toString(),event).get(10,TimeUnit.SECONDS);
                db.update("UPDATE outbox_eventos SET publicado=1 WHERE id=?",event.eventId().toString());
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                log.warn("Publicacion outbox interrumpida; evento pendiente id={}", event.eventId());
                break;
            } catch (Exception error) {
                // Conserva evento pendiente para el siguiente intento sin detener el servicio.
                log.warn("Publicacion outbox pendiente id={} causa={}", event.eventId(), error.getClass().getSimpleName());
                break;
            }
        }
    }
}
