package cl.duoc.bancoxyzbatch.config;

import cl.duoc.bancoxyzbatch.listener.RejectedItemSkipListener;
import cl.duoc.bancoxyzbatch.listener.TransaccionesJobListener;
import cl.duoc.bancoxyzbatch.model.TransaccionProcesada;
import cl.duoc.bancoxyzbatch.model.TransaccionRaw;
import cl.duoc.bancoxyzbatch.partition.LineRangePartitioner;
import cl.duoc.bancoxyzbatch.processor.TransaccionProcessor;
import cl.duoc.bancoxyzbatch.support.BancoSkipPolicy;
import cl.duoc.bancoxyzbatch.support.SourceDataCounter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.configuration.annotation.StepScope;
import javax.sql.DataSource;

@Configuration
public class TransaccionesJobConfig {
    private static final String SOURCE = "data/semana_3/movimientos_financieros_diarios.csv";

    @Bean
    @StepScope
    public FlatFileItemReader<TransaccionRaw> transaccionesReader(
            @Value("#{stepExecutionContext['startIndex']}") Integer startIndex,
            @Value("#{stepExecutionContext['itemCount']}") Integer itemCount) {
        return new FlatFileItemReaderBuilder<TransaccionRaw>()
            .name("transaccionesReader")
            .resource(new ClassPathResource(SOURCE))
            .linesToSkip(1 + startIndex)
            .maxItemCount(itemCount)
            .delimited().delimiter(",").names("id", "fecha", "monto", "tipo")
            .fieldSetMapper(fs -> new TransaccionRaw(
                fs.readString("id"), fs.readString("fecha"), fs.readString("monto"), fs.readString("tipo")))
            .build();
    }

    @Bean
    public ItemWriter<TransaccionProcesada> transaccionesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<TransaccionProcesada>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO transacciones_procesadas(source_id, fecha, monto, tipo, monto_firmado)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE fecha=VALUES(fecha), monto=VALUES(monto), tipo=VALUES(tipo), monto_firmado=VALUES(monto_firmado)
                """)
            .itemPreparedStatementSetter((item, ps) -> {
                ps.setLong(1, item.sourceId());
                ps.setObject(2, item.fecha());
                ps.setBigDecimal(3, item.monto());
                ps.setString(4, item.tipo());
                ps.setBigDecimal(5, item.montoFirmado());
            })
            .build();
    }

    @Bean
    public Step transaccionesWorkerStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<TransaccionRaw> transaccionesReader,
            TransaccionProcessor processor,
            ItemWriter<TransaccionProcesada> transaccionesWriter,
            BancoSkipPolicy bancoSkipPolicy,
            BatchExecutionProperties properties,
            BackOffPolicy retryBackOffPolicy,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("transaccionesWorkerStep", jobRepository)
            .<TransaccionRaw, TransaccionProcesada>chunk(properties.getChunkSize(), transactionManager)
            .reader(transaccionesReader)
            .processor(processor)
            .writer(transaccionesWriter)
            .faultTolerant()
            .processorNonTransactional()
            .skipPolicy(bancoSkipPolicy)
            .retry(TransientDataAccessException.class)
            .retryLimit(properties.getRetryLimit())
            .backOffPolicy(retryBackOffPolicy)
            .listener(new RejectedItemSkipListener<TransaccionRaw, TransaccionProcesada>(TransaccionProcessor.JOB_NAME, jdbcTemplate))
            .build();
    }

    @Bean
    public Step transaccionesPartitionStep(JobRepository jobRepository,
            @Qualifier("transaccionesWorkerStep") Step workerStep,
            SourceDataCounter counter,
            BatchExecutionProperties properties,
            @Qualifier("batchTaskExecutor") TaskExecutor taskExecutor) {
        return new StepBuilder("transaccionesPartitionStep", jobRepository)
            .partitioner(workerStep.getName(), new LineRangePartitioner(SOURCE, counter))
            .step(workerStep)
            .gridSize(properties.getPartition().getGridSize())
            .taskExecutor(taskExecutor)
            .build();
    }

    @Bean
    public Job transaccionesJob(JobRepository jobRepository,
            @Qualifier("transaccionesPartitionStep") Step partitionStep,
            TransaccionesJobListener listener) {
        return new JobBuilder(TransaccionProcessor.JOB_NAME, jobRepository)
            .listener(listener)
            .start(partitionStep)
            .build();
    }
}
