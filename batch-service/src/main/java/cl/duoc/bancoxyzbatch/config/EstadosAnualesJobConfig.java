package cl.duoc.bancoxyzbatch.config;

import cl.duoc.bancoxyzbatch.listener.EstadosAnualesJobListener;
import cl.duoc.bancoxyzbatch.listener.RejectedItemSkipListener;
import cl.duoc.bancoxyzbatch.model.CuentaAnualRaw;
import cl.duoc.bancoxyzbatch.model.MovimientoAnual;
import cl.duoc.bancoxyzbatch.partition.LineRangePartitioner;
import cl.duoc.bancoxyzbatch.processor.CuentaAnualProcessor;
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
public class EstadosAnualesJobConfig {
    private static final String SOURCE = "data/semana_3/estados_financieros_anuales.csv";

    @Bean
    @StepScope
    public FlatFileItemReader<CuentaAnualRaw> cuentasAnualesReader(
            @Value("#{stepExecutionContext['startIndex']}") Integer startIndex,
            @Value("#{stepExecutionContext['itemCount']}") Integer itemCount) {
        return new FlatFileItemReaderBuilder<CuentaAnualRaw>()
            .name("cuentasAnualesReader")
            .resource(new ClassPathResource(SOURCE))
            .linesToSkip(1 + startIndex)
            .maxItemCount(itemCount)
            .delimited().delimiter(",").names("cuenta_id", "fecha", "transaccion", "monto", "descripcion")
            .fieldSetMapper(fs -> new CuentaAnualRaw(
                fs.readString("cuenta_id"), fs.readString("fecha"), fs.readString("transaccion"),
                fs.readString("monto"), fs.readString("descripcion")))
            .build();
    }

    @Bean
    public ItemWriter<MovimientoAnual> movimientosAnualesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MovimientoAnual>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO movimientos_anuales(source_hash, cuenta_id, fecha, transaccion, monto, descripcion, monto_firmado)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE fecha=VALUES(fecha), transaccion=VALUES(transaccion), monto=VALUES(monto), descripcion=VALUES(descripcion), monto_firmado=VALUES(monto_firmado)
                """)
            .itemPreparedStatementSetter((item, ps) -> {
                ps.setString(1, item.sourceHash());
                ps.setLong(2, item.cuentaId());
                ps.setObject(3, item.fecha());
                ps.setString(4, item.transaccion());
                ps.setBigDecimal(5, item.monto());
                ps.setString(6, item.descripcion());
                ps.setBigDecimal(7, item.montoFirmado());
            })
            .build();
    }

    @Bean
    public Step estadosAnualesWorkerStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<CuentaAnualRaw> cuentasAnualesReader,
            CuentaAnualProcessor processor,
            ItemWriter<MovimientoAnual> movimientosAnualesWriter,
            BancoSkipPolicy bancoSkipPolicy,
            BatchExecutionProperties properties,
            BackOffPolicy retryBackOffPolicy,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("estadosAnualesWorkerStep", jobRepository)
            .<CuentaAnualRaw, MovimientoAnual>chunk(properties.getChunkSize(), transactionManager)
            .reader(cuentasAnualesReader)
            .processor(processor)
            .writer(movimientosAnualesWriter)
            .faultTolerant()
            .processorNonTransactional()
            .skipPolicy(bancoSkipPolicy)
            .retry(TransientDataAccessException.class)
            .retryLimit(properties.getRetryLimit())
            .backOffPolicy(retryBackOffPolicy)
            .listener(new RejectedItemSkipListener<CuentaAnualRaw, MovimientoAnual>(CuentaAnualProcessor.JOB_NAME, jdbcTemplate))
            .build();
    }

    @Bean
    public Step estadosAnualesPartitionStep(JobRepository jobRepository,
            @Qualifier("estadosAnualesWorkerStep") Step workerStep,
            SourceDataCounter counter,
            BatchExecutionProperties properties,
            @Qualifier("batchTaskExecutor") TaskExecutor taskExecutor) {
        return new StepBuilder("estadosAnualesPartitionStep", jobRepository)
            .partitioner(workerStep.getName(), new LineRangePartitioner(SOURCE, counter))
            .step(workerStep)
            .gridSize(properties.getPartition().getGridSize())
            .taskExecutor(taskExecutor)
            .build();
    }

    @Bean
    public Job estadosAnualesJob(JobRepository jobRepository,
            @Qualifier("estadosAnualesPartitionStep") Step partitionStep,
            EstadosAnualesJobListener listener) {
        return new JobBuilder(CuentaAnualProcessor.JOB_NAME, jobRepository)
            .listener(listener)
            .start(partitionStep)
            .build();
    }
}
