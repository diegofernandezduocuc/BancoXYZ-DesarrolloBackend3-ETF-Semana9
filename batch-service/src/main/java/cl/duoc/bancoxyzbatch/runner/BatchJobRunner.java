package cl.duoc.bancoxyzbatch.runner;

import cl.duoc.bancoxyzbatch.config.BatchExecutionProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BatchJobRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BatchJobRunner.class);
    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobs = new LinkedHashMap<>();
    private final BatchExecutionProperties properties;

    public BatchJobRunner(JobLauncher jobLauncher,
            @Qualifier("transaccionesJob") Job transaccionesJob,
            @Qualifier("interesesJob") Job interesesJob,
            @Qualifier("estadosAnualesJob") Job estadosAnualesJob,
            BatchExecutionProperties properties) {
        this.jobLauncher = jobLauncher;
        this.properties = properties;
        jobs.put("transacciones", transaccionesJob);
        jobs.put("intereses", interesesJob);
        jobs.put("anuales", estadosAnualesJob);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String selected = option(args, "job", "todos").toLowerCase(Locale.ROOT);
        long start = System.nanoTime();
        // Un identificador estable permite retomar una ejecución fallida con los mismos parámetros.
        String runId = option(args, "run-id", Long.toString(System.nanoTime()));

        if (selected.equals("todos")) {
            for (Map.Entry<String, Job> entry : jobs.entrySet()) {
                execute(entry.getKey(), entry.getValue(), runId);
            }
        } else {
            Job job = jobs.get(selected);
            if (job == null) {
                throw new IllegalArgumentException("Job desconocido: " + selected + ". Valores: transacciones, intereses, anuales, todos");
            }
            execute(selected, job, runId);
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        log.info("BENCHMARK_RESULT job={} gridSize={} chunkSize={} durationMs={} status=COMPLETED",
            selected, properties.getPartition().getGridSize(), properties.getChunkSize(), durationMs);
    }

    private void execute(String alias, Job job, String runId) throws Exception {
        var parameters = new JobParametersBuilder()
                .addString("run.id", runId)
                .addString("alias", alias)
                .toJobParameters();
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.info("INICIO_JOB alias={} jobName={} intento={}", alias, job.getName(), attempt);
            try {
                JobExecution execution = jobLauncher.run(job, parameters);
                if (execution.getStatus() == BatchStatus.COMPLETED) {
                    log.info("FIN_JOB alias={} status=COMPLETED intento={}", alias, attempt);
                    return;
                }
                if (attempt == maxAttempts) {
                    throw new IllegalStateException("El job " + job.getName() + " terminó con estado " + execution.getStatus());
                }
                log.warn("Reejecución de job {} luego de estado {}",alias,execution.getStatus());
            } catch (Exception ex) {
                if (attempt == maxAttempts) throw ex;
                log.warn("Error en job {} intento {}. Se intentará nuevamente: {}",alias,attempt,ex.getMessage());
            }
            Thread.sleep(1000L * attempt);
        }
    }

    private String option(ApplicationArguments args, String name, String defaultValue) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? defaultValue : values.get(0);
    }
}
