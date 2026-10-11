package cl.duoc.bancoxyzbatch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.jdbc.core.JdbcTemplate;

public class RejectedItemSkipListener<I, O> implements SkipListener<I, O> {
    private static final Logger log = LoggerFactory.getLogger(RejectedItemSkipListener.class);
    private final String jobName;
    private final JdbcTemplate jdbcTemplate;

    public RejectedItemSkipListener(String jobName, JdbcTemplate jdbcTemplate) {
        this.jobName = jobName;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        persist("Error de lectura", t);
    }

    @Override
    public void onSkipInProcess(I item, Throwable t) {
        persist(String.valueOf(item), t);
    }

    @Override
    public void onSkipInWrite(O item, Throwable t) {
        persist(String.valueOf(item), t);
    }

    private void persist(String raw, Throwable t) {
        String reason = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        log.warn("{} - registro omitido: {} - motivo: {}", jobName, raw, reason);
        jdbcTemplate.update(
            "INSERT INTO registros_rechazados(job_name, raw_data, motivo) VALUES (?, ?, ?)",
            jobName, truncate(raw, 1500), truncate(reason, 500));
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
