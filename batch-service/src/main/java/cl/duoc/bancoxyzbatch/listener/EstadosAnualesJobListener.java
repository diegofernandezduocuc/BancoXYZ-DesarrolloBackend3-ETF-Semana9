package cl.duoc.bancoxyzbatch.listener;

import cl.duoc.bancoxyzbatch.processor.CuentaAnualProcessor;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EstadosAnualesJobListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(EstadosAnualesJobListener.class);
    private final JdbcTemplate jdbcTemplate;
    private final JobExplorer jobExplorer;
    private final DuplicateRegistry duplicates;

    public EstadosAnualesJobListener(JdbcTemplate jdbcTemplate, DuplicateRegistry duplicates, JobExplorer jobExplorer) {
        this.jdbcTemplate = jdbcTemplate;
        this.duplicates = duplicates;
        this.jobExplorer = jobExplorer;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        duplicates.clear(CuentaAnualProcessor.JOB_NAME);
        // No borrar resultados parciales al reiniciar una ejecución fallida.
        if (jobExplorer.getJobExecutions(jobExecution.getJobInstance()).size() > 1) {
            return;
        }
        jdbcTemplate.update("DELETE FROM estados_cuenta_anuales");
        jdbcTemplate.update("DELETE FROM movimientos_anuales");
        jdbcTemplate.update("DELETE FROM registros_rechazados WHERE job_name = ?", CuentaAnualProcessor.JOB_NAME);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) return;
        jdbcTemplate.update("""
            INSERT INTO estados_cuenta_anuales(
                cuenta_id, total_operaciones, total_ingresos, total_egresos,
                saldo_neto_anual, fecha_primera, fecha_ultima)
            SELECT cuenta_id,
                   COUNT(*),
                   COALESCE(SUM(CASE WHEN monto_firmado > 0 THEN monto_firmado ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN monto_firmado < 0 THEN ABS(monto_firmado) ELSE 0 END), 0),
                   COALESCE(SUM(monto_firmado), 0),
                   MIN(fecha),
                   MAX(fecha)
            FROM movimientos_anuales
            GROUP BY cuenta_id
            """);

        Integer movements = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movimientos_anuales", Integer.class);
        Integer accounts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM estados_cuenta_anuales", Integer.class);
        Integer rejected = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM registros_rechazados WHERE job_name = ?", Integer.class, CuentaAnualProcessor.JOB_NAME);
        log.info("RESUMEN_JOB estados_anuales movimientos_validos={} cuentas_consolidadas={} rechazados={} status={}",
            movements, accounts, rejected, jobExecution.getStatus());
    }
}
