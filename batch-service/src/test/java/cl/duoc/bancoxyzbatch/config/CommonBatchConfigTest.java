package cl.duoc.bancoxyzbatch.config;

import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.TransientDataAccessResourceException;

import static org.junit.jupiter.api.Assertions.*;

class CommonBatchConfigTest {
    @Test
    void filaConColumnasFaltantesSePuedeOmitirYLeerLaSiguiente() throws Exception {
        var reader = new TransaccionesJobConfig().transaccionesReader(0, 2);
        reader.setResource(new ByteArrayResource(("id,fecha,monto,tipo\n"
                + "1,2024-04-09,100\n2,2024-04-09,50,credito\n")
                .getBytes(StandardCharsets.UTF_8)));
        var policy = new CommonBatchConfig().bancoSkipPolicy(new BatchExecutionProperties());
        try {
            reader.open(new ExecutionContext());
            var failure = assertThrows(FlatFileParseException.class, reader::read);
            assertTrue(policy.shouldSkip(failure, 0));
            assertEquals("2", reader.read().id());
            assertNull(reader.read());
        } finally {
            reader.close();
        }
    }

    @Test
    void conservaLimiteDeRechazosYValidacionesDelProcesador() {
        var properties = new BatchExecutionProperties();
        properties.setSkipLimit(2);
        var policy = new CommonBatchConfig().bancoSkipPolicy(properties);
        var failure = new FlatFileParseException("Fila invalida", "1,2024-04-09,100", 2);
        assertTrue(policy.shouldSkip(failure, -1));
        assertTrue(policy.shouldSkip(failure, 1));
        assertFalse(policy.shouldSkip(failure, 2));
        assertTrue(policy.shouldSkip(new InvalidDataException("Monto invalido"), 0));
    }

    @Test
    void erroresDeInfraestructuraYProgramacionNoSeOmiten() {
        var policy = new CommonBatchConfig().bancoSkipPolicy(new BatchExecutionProperties());
        assertFalse(policy.shouldSkip(new IOException("Fuente inaccesible"), 0));
        assertFalse(policy.shouldSkip(new TransientDataAccessResourceException("BD inaccesible"), 0));
        assertFalse(policy.shouldSkip(new IllegalStateException("Configuracion invalida"), 0));
    }
}
