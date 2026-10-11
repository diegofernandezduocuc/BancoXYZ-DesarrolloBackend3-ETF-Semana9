package cl.duoc.bancoxyzbatch.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SourceDataCounter {
    public int countDataRows(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            long lines = reader.lines().count();
            return Math.max(0, Math.toIntExact(lines - 1));
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible contar filas de " + classpathLocation, e);
        }
    }
}
