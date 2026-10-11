package cl.duoc.bancoxyzbatch.listener;

import cl.duoc.bancoxyzbatch.processor.InteresProcessor;
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
public class InteresesJobListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(InteresesJobListener.class);
    private final JdbcTemplate jdbcTemplate;
    private final JobExplorer jobExplorer;
    private final DuplicateRegistry duplicates;

    public InteresesJobListener(JdbcTemplate jdbcTemplate, DuplicateRegistry duplicates, JobExplorer jobExplorer) {
        this.jdbcTemplate = jdbcTemplate;
        this.duplicates = duplicates;
        this.jobExplorer = jobExplorer;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        duplicates.clear(InteresProcessor.JOB_NAME);
        // No borrar resultados parciales al reiniciar una ejecución fallida.
        if (jobExplorer.getJobExecutions(jobExecution.getJobInstance()).size() > 1) {
            return;
        }
        jdbcTemplate.update("DELETE FROM intereses_calculados");
        jdbcTemplate.update("DELETE FROM registros_rechazados WHERE job_name = ?", InteresProcessor.JOB_NAME);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) return;
        Integer valid = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intereses_calculados", Integer.class);
        Integer rejected = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM registros_rechazados WHERE job_name = ?", Integer.class, InteresProcessor.JOB_NAME);
        log.info("RESUMEN_JOB intereses validos={} rechazados={} status={}", valid, rejected, jobExecution.getStatus());
    }
}
