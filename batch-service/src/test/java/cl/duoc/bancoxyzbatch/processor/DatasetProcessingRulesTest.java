package cl.duoc.bancoxyzbatch.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cl.duoc.bancoxyzbatch.config.InterestRateProperties;
import cl.duoc.bancoxyzbatch.exception.InvalidDataException;
import cl.duoc.bancoxyzbatch.model.CuentaAnualRaw;
import cl.duoc.bancoxyzbatch.model.InteresRaw;
import cl.duoc.bancoxyzbatch.model.TransaccionRaw;
import cl.duoc.bancoxyzbatch.support.DuplicateRegistry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DatasetProcessingRulesTest {

    @Test
    void reglasProcesanDatasetCompletoConConteosEsperados() throws Exception {
        DuplicateRegistry registry = new DuplicateRegistry();
        TransaccionProcessor txProcessor = new TransaccionProcessor(registry);
        InteresProcessor interesesProcessor = new InteresProcessor(registry, new InterestRateProperties());
        CuentaAnualProcessor anualProcessor = new CuentaAnualProcessor(registry);

        Counts tx = processTransacciones(txProcessor);
        Counts interests = processIntereses(interesesProcessor);
        Counts annual = processAnuales(anualProcessor);
        assertEquals(new Counts(392, 608), tx);
        assertEquals(new Counts(320, 680), interests);
        assertEquals(new Counts(497, 503), annual);
    }

    private Counts processTransacciones(TransaccionProcessor processor) throws Exception {
        return process("data/semana_3/movimientos_financieros_diarios.csv", 4, values ->
            processor.process(new TransaccionRaw(values[0], values[1], values[2], values[3])));
    }

    private Counts processIntereses(InteresProcessor processor) throws Exception {
        return process("data/semana_3/intereses_trimestrales.csv", 5, values ->
            processor.process(new InteresRaw(values[0], values[1], values[2], values[3], values[4])));
    }

    private Counts processAnuales(CuentaAnualProcessor processor) throws Exception {
        return process("data/semana_3/estados_financieros_anuales.csv", 5, values ->
            processor.process(new CuentaAnualRaw(values[0], values[1], values[2], values[3], values[4])));
    }

    private Counts process(String location, int fields, ThrowingConsumer consumer) throws Exception {
        int valid = 0;
        int rejected = 0;
        ClassPathResource resource = new ClassPathResource(location);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                if (values.length != fields) {
                    rejected++;
                    continue;
                }
                try {
                    consumer.accept(values);
                    valid++;
                } catch (InvalidDataException e) {
                    rejected++;
                }
            }
        }
        return new Counts(valid, rejected);
    }

    @FunctionalInterface
    interface ThrowingConsumer {
        void accept(String[] values);
    }

    record Counts(int valid, int rejected) { }
}
