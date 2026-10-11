package cl.duoc.bancoxyz.web.client;

import cl.duoc.bancoxyz.web.exception.BackendUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BackendClient {
    /** Los rechazos bancarios previstos no indican que el servicio este caido. */
    public static final class BusinessFailurePredicate implements java.util.function.Predicate<Throwable> {
        @Override
        public boolean test(Throwable failure) {
            if (!(failure instanceof ResponseStatusException response)) return false;
            int status = response.getStatusCode().value();
            return status == 400 || status == 404 || status == 409;
        }
    }


    private final AtomicInteger nextInstance = new AtomicInteger();
    private final DiscoveryClient discoveryClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final String serviceId;

    public BackendClient(DiscoveryClient discoveryClient,
                         CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                         @Value("${backend.service-id:bancoxyz-backend-core}") String serviceId) {
        this.discoveryClient = discoveryClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.serviceId = serviceId;
    }

    public List<BackendCuenta> getCuentas() {
        String accessToken = currentAccessToken();
        return circuitBreakerFactory.create("backendCore").run(
                () -> restClient(accessToken).get().uri("/api/backend/cuentas").retrieve()
                        .body(new ParameterizedTypeReference<>() {}),
                throwable -> { throw translateFailure(throwable); });
    }

    public BackendCuenta getCuenta(Long id) {
        String accessToken = currentAccessToken();
        return circuitBreakerFactory.create("backendCore").run(
                () -> restClient(accessToken).get().uri("/api/backend/cuentas/{id}", id).retrieve()
                        .onStatus(status -> status.value() == 404,
                                (request, response) -> {
                                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
                                })
                        .body(BackendCuenta.class),
                throwable -> { throw translateFailure(throwable); });
    }

    public List<BackendMovimiento> getMovimientos(Long id, int limit) {
        String accessToken = currentAccessToken();
        return circuitBreakerFactory.create("backendCore").run(
                () -> restClient(accessToken).get()
                        .uri(uriBuilder -> uriBuilder.path("/api/backend/cuentas/{id}/movimientos")
                                .queryParam("limit", limit).build(id))
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {}),
                throwable -> { throw translateFailure(throwable); });
    }

    public BackendResumenTransacciones getResumenTransacciones() {
        String accessToken = currentAccessToken();
        return circuitBreakerFactory.create("backendCore").run(
                () -> restClient(accessToken).get().uri("/api/backend/resumen/transacciones").retrieve()
                        .body(BackendResumenTransacciones.class),
                throwable -> { throw translateFailure(throwable); });
    }

    private String currentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT no disponible");
    }

    private RestClient restClient(String accessToken) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (instances.isEmpty()) throw new BackendUnavailableException("Backend Core no está registrado en Eureka");
        ServiceInstance instance = instances.get(Math.floorMod(nextInstance.getAndIncrement(), instances.size()));

        return RestClient.builder()
                .baseUrl(instance.getUri().toString())
                .defaultHeaders(headers -> headers.setBearerAuth(accessToken))
                .build();
    }

    private RuntimeException translateFailure(Throwable throwable) {
        if (throwable instanceof ResponseStatusException responseStatusException) {
            return responseStatusException;
        }
        return new BackendUnavailableException("Servicio de datos temporalmente no disponible", throwable);
    }
}
