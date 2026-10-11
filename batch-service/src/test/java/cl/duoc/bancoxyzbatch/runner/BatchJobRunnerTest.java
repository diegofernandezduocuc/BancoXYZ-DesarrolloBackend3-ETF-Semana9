package cl.duoc.bancoxyzbatch.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.bancoxyzbatch.config.BatchExecutionProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.DefaultApplicationArguments;

class BatchJobRunnerTest {
    @Test
    void reutilizaParametrosEnReintentoDelMismoJob() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job transacciones = mock(Job.class);
        Job intereses = mock(Job.class);
        Job anuales = mock(Job.class);
        JobExecution fallido = mock(JobExecution.class);
        JobExecution completo = mock(JobExecution.class);
        when(transacciones.getName()).thenReturn("transaccionesJob");
        when(fallido.getStatus()).thenReturn(BatchStatus.FAILED);
        when(completo.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(launcher.run(eq(transacciones), any(JobParameters.class))).thenReturn(fallido, completo);

        var runner = new BatchJobRunner(launcher, transacciones, intereses, anuales,
                new BatchExecutionProperties());
        runner.run(new DefaultApplicationArguments("--job=transacciones", "--run-id=prueba-reinicio"));

        ArgumentCaptor<JobParameters> parametros = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher, times(2)).run(eq(transacciones), parametros.capture());
        assertEquals(parametros.getAllValues().get(0), parametros.getAllValues().get(1));
        assertEquals("prueba-reinicio", parametros.getValue().getString("run.id"));
    }
}
