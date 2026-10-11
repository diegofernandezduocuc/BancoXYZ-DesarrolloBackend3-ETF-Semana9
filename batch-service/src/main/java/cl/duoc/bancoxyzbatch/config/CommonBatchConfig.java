package cl.duoc.bancoxyzbatch.config;

import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.support.BancoSkipPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CommonBatchConfig {

    @Bean
    public TaskExecutor batchTaskExecutor(BatchExecutionProperties properties) {
        int gridSize = Math.max(1, properties.getPartition().getGridSize());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(gridSize);
        executor.setMaxPoolSize(gridSize);
        executor.setQueueCapacity(gridSize * 2);
        executor.setThreadNamePrefix("batch-worker-");
        executor.initialize();
        return executor;
    }

    @Bean
    public BancoSkipPolicy bancoSkipPolicy(BatchExecutionProperties properties) {
        return new BancoSkipPolicy(properties.getSkipLimit(), InvalidDataException.class, FlatFileParseException.class);
    }

    @Bean
    public FixedBackOffPolicy retryBackOffPolicy(BatchExecutionProperties properties) {
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(properties.getRetryBackoffMs());
        return policy;
    }
}
