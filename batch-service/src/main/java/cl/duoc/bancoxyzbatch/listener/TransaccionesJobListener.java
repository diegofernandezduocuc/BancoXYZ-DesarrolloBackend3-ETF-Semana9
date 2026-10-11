package cl.duoc.bancoxyzbatch.listener;

import cl.duoc.bancoxyzbatch.processor.TransaccionProcessor;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import cl.duoc.bancoxyzbatch.support.SourceDataCounter;
import java.math.BigDecimal;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.BatchStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransaccionesJobListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(TransaccionesJobListener.class);
    private static final String SOURCE = "data/semana_3/movimientos_financieros_diarios.csv";
    private final JdbcTemplate jdbcTemplate;
    private final JobExplorer jobExplorer;
    private final DuplicateRegistry duplicates;
    private final SourceDataCounter counter;

    public TransaccionesJobListener(JdbcTemplate jdbcTemplate, DuplicateRegistry duplicates, SourceDataCounter counter, JobExplorer jobExplorer) {
        this.jdbcTemplate = jdbcTemplate;
        this.duplicates = duplicates;
        this.counter = counter;
        this.jobExplorer = jobExplorer;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        duplicates.clear(TransaccionProcessor.JOB_NAME);
        // No borrar resultados parciales al reiniciar una ejecución fallida.
        if (jobExplorer.getJobExecutions(jobExecution.getJobInstance()).size() > 1) {
            return;
        }
        jdbcTemplate.update("DELETE FROM resumen_transacciones");
        jdbcTemplate.update("DELETE FROM transacciones_procesadas");
        jdbcTemplate.update("DELETE FROM registros_rechazados WHERE job_name = ?", TransaccionProcessor.JOB_NAME);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            return;
        }
        int input = counter.countDataRows(SOURCE);
        Integer valid = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transacciones_procesadas", Integer.class);
        Integer rejected = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM registros_rechazados WHERE job_name = ?", Integer.class, TransaccionProcessor.JOB_NAME);
        BigDecimal credits = amount("credito");
        BigDecimal debits = amount("debito");
        BigDecimal net = credits.subtract(debits);
        long durationMs = duration(jobExecution);

        jdbcTemplate.update("""
            INSERT INTO resumen_transacciones(registros_entrada, registros_validos, anomalias_detectadas,
                total_creditos, total_debitos, movimiento_neto, duracion_ms)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, input, valid, rejected, credits, debits, net, durationMs);

        log.info("RESUMEN_JOB transacciones entrada={} validos={} anomalias={} creditos={} debitos={} neto={} durationMs={} status={}",
            input, valid, rejected, credits, debits, net, durationMs, jobExecution.getStatus());
    }

    private BigDecimal amount(String type) {
        BigDecimal value = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(monto), 0) FROM transacciones_procesadas WHERE tipo = ?", BigDecimal.class, type);
        return value == null ? BigDecimal.ZERO : value;
    }

    private long duration(JobExecution execution) {
        if (execution.getStartTime() == null || execution.getEndTime() == null) return 0L;
        return Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();
    }
}
