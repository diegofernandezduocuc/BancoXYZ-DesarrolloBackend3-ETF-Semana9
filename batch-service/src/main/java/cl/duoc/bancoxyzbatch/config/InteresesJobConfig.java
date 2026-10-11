package cl.duoc.bancoxyzbatch.config;

import cl.duoc.bancoxyzbatch.listener.InteresesJobListener;
import cl.duoc.bancoxyzbatch.listener.RejectedItemSkipListener;
import cl.duoc.bancoxyzbatch.model.InteresCalculado;
import cl.duoc.bancoxyzbatch.model.InteresRaw;
import cl.duoc.bancoxyzbatch.partition.LineRangePartitioner;
import cl.duoc.bancoxyzbatch.processor.InteresProcessor;
import cl.duoc.bancoxyzbatch.support.BancoSkipPolicy;
import cl.duoc.bancoxyzbatch.support.SourceDataCounter;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
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

@Configuration
public class InteresesJobConfig {
    private static final String SOURCE = "data/semana_3/intereses_trimestrales.csv";

    @Bean
    @StepScope
    public FlatFileItemReader<InteresRaw> interesesReader(
            @Value("#{stepExecutionContext['startIndex']}") Integer startIndex,
            @Value("#{stepExecutionContext['itemCount']}") Integer itemCount) {
        return new FlatFileItemReaderBuilder<InteresRaw>()
            .name("interesesReader")
            .resource(new ClassPathResource(SOURCE))
            .linesToSkip(1 + startIndex)
            .maxItemCount(itemCount)
            .delimited().delimiter(",").names("cuenta_id", "nombre", "saldo", "edad", "tipo")
            .fieldSetMapper(fs -> new InteresRaw(
                fs.readString("cuenta_id"), fs.readString("nombre"), fs.readString("saldo"),
                fs.readString("edad"), fs.readString("tipo")))
            .build();
    }

    @Bean
    public ItemWriter<InteresCalculado> interesesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<InteresCalculado>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO intereses_calculados(source_hash, cuenta_id, nombre, saldo_inicial, edad, tipo, tasa, interes, saldo_final)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), saldo_inicial=VALUES(saldo_inicial), edad=VALUES(edad), tipo=VALUES(tipo), tasa=VALUES(tasa), interes=VALUES(interes), saldo_final=VALUES(saldo_final)
                """)
            .itemPreparedStatementSetter((item, ps) -> {
                ps.setString(1, item.sourceHash());
                ps.setLong(2, item.cuentaId());
                ps.setString(3, item.nombre());
                ps.setBigDecimal(4, item.saldoInicial());
                ps.setInt(5, item.edad());
                ps.setString(6, item.tipo());
                ps.setBigDecimal(7, item.tasa());
                ps.setBigDecimal(8, item.interes());
                ps.setBigDecimal(9, item.saldoFinal());
            })
            .build();
    }

    @Bean
    public Step interesesWorkerStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<InteresRaw> interesesReader,
            InteresProcessor processor,
            ItemWriter<InteresCalculado> interesesWriter,
            BancoSkipPolicy bancoSkipPolicy,
            BatchExecutionProperties properties,
            BackOffPolicy retryBackOffPolicy,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("interesesWorkerStep", jobRepository)
            .<InteresRaw, InteresCalculado>chunk(properties.getChunkSize(), transactionManager)
            .reader(interesesReader)
            .processor(processor)
            .writer(interesesWriter)
            .faultTolerant()
            .processorNonTransactional()
            .skipPolicy(bancoSkipPolicy)
            .retry(TransientDataAccessException.class)
            .retryLimit(properties.getRetryLimit())
            .backOffPolicy(retryBackOffPolicy)
            .listener(new RejectedItemSkipListener<InteresRaw, InteresCalculado>(InteresProcessor.JOB_NAME, jdbcTemplate))
            .build();
    }

    @Bean
    public Step interesesPartitionStep(JobRepository jobRepository,
            @Qualifier("interesesWorkerStep") Step workerStep,
            SourceDataCounter counter,
            BatchExecutionProperties properties,
            @Qualifier("batchTaskExecutor") TaskExecutor taskExecutor) {
        return new StepBuilder("interesesPartitionStep", jobRepository)
            .partitioner(workerStep.getName(), new LineRangePartitioner(SOURCE, counter))
            .step(workerStep)
            .gridSize(properties.getPartition().getGridSize())
            .taskExecutor(taskExecutor)
            .build();
    }

    @Bean
    public Job interesesJob(JobRepository jobRepository,
            @Qualifier("interesesPartitionStep") Step partitionStep,
            InteresesJobListener listener) {
        return new JobBuilder(InteresProcessor.JOB_NAME, jobRepository)
            .listener(listener)
            .start(partitionStep)
            .build();
    }
}
