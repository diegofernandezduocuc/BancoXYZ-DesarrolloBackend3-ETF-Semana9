package cl.duoc.bancoxyz.pagos;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerBusinessErrorsTest {
    private CircuitBreakerRegistry registry() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("../config-server/src/main/resources/config-repo/bancoxyz-pagos-service.yml"));
        var properties = new Binder(new MapConfigurationPropertySource(yaml.getObject()))
                .bind("resilience4j.circuitbreaker", Bindable.of(CommonCircuitBreakerConfigurationProperties.class))
                .orElseGet(CommonCircuitBreakerConfigurationProperties::new);
        var instance = properties.getInstances().getOrDefault("cuentasService",
                new CommonCircuitBreakerConfigurationProperties.InstanceProperties());
        var config = properties.createCircuitBreakerConfig("cuentasService", instance, new CompositeCustomizer<>(List.of()));
        return CircuitBreakerRegistry.of(config);
    }

    @Test
    void rechazosDeNegocioNoAbrenCircuitoNiCambianElError() {
        var registry = registry();
        var executor = Executors.newSingleThreadExecutor();
        try {
            var factory = new Resilience4JCircuitBreakerFactory(registry, TimeLimiterRegistry.ofDefaults(), null);
            factory.configureExecutorService(executor);
            var breaker = factory.create("cuentasService");
            for (var status : List.of(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND, HttpStatus.CONFLICT,
                    HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND, HttpStatus.CONFLICT)) {
                var expected = new ResponseStatusException(status, "Rechazo de prueba");
                Throwable actual = breaker.run(() -> { throw expected; }, failure -> failure);
                assertSame(expected, actual);
            }
            assertEquals(0, registry.circuitBreaker("cuentasService").getMetrics().getNumberOfFailedCalls());
            assertEquals("ok", breaker.run(() -> "ok", failure -> "fallback"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void infraestructuraYErroresDeAccesoSiguenContandoComoFallos() {
        var registry = registry();
        var executor = Executors.newSingleThreadExecutor();
        try {
            var factory = new Resilience4JCircuitBreakerFactory(registry, TimeLimiterRegistry.ofDefaults(), null);
            factory.configureExecutorService(executor);
            var breaker = factory.create("cuentasService");
            var unavailable = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            assertSame(unavailable, breaker.run(() -> { throw unavailable; }, failure -> failure));
            var network = new ResourceAccessException("Conexion de prueba fallida");
            assertSame(network, breaker.run(() -> { throw network; }, failure -> failure));
            assertEquals(2, registry.circuitBreaker("cuentasService").getMetrics().getNumberOfFailedCalls());
            var predicate = registry.getDefaultConfig().getIgnoreExceptionPredicate();
            assertFalse(predicate.test(new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
            assertFalse(predicate.test(new ResponseStatusException(HttpStatus.FORBIDDEN)));
        } finally {
            executor.shutdownNow();
        }
    }
}

