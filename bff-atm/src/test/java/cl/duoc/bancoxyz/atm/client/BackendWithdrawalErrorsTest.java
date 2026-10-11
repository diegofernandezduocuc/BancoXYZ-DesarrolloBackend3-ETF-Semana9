package cl.duoc.bancoxyz.atm.client;

import cl.duoc.bancoxyz.atm.exception.BackendUnavailableException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BackendWithdrawalErrorsTest {
    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409, 503})
    void preservaRechazosYFallbackTecnico(int status) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://test.invalid:80/api/backend/cuentas/1/retiros"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        var discovery = mock(DiscoveryClient.class);
        when(discovery.getInstances("backend")).thenReturn(List.of(
                new DefaultServiceInstance("test", "backend", "test.invalid", 80, false)));
        var factory = mock(CircuitBreakerFactory.class);
        // Ejecuta solo el adaptador HTTP en este hilo; el circuito real tiene sus propias pruebas.
        when(factory.create("backendCore")).thenReturn(new CircuitBreaker() {
            @Override public <T> T run(Supplier<T> supplier, Function<Throwable, T> fallback) {
                try { return supplier.get(); } catch (RuntimeException ex) { return fallback.apply(ex); }
            }
        });
        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("unit-test");
        var authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        var previous = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try (var rest = mockStatic(RestClient.class)) {
            rest.when(RestClient::builder).thenReturn(builder);
            var client = new BackendClient(discovery, factory, "backend");
            if (status == 503) {
                assertThrows(BackendUnavailableException.class,
                        () -> client.retirar(1L, BigDecimal.TEN));
            } else {
                var error = assertThrows(ResponseStatusException.class,
                        () -> client.retirar(1L, new BigDecimal("0.015")));
                assertEquals(status, error.getStatusCode().value());
            }
        } finally {
            SecurityContextHolder.setContext(previous);
        }
        server.verify();
    }
}
