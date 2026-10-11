package cl.duoc.bancoxyz.pagos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/pagos")
public class PagosController {
    /** Los rechazos bancarios previstos no indican que el servicio este caido. */
    public static final class BusinessFailurePredicate implements java.util.function.Predicate<Throwable> {
        @Override
        public boolean test(Throwable failure) {
            if (!(failure instanceof ResponseStatusException response)) return false;
            int status = response.getStatusCode().value();
            return status == 400 || status == 404 || status == 409;
        }
    }

    public record Monto(@NotNull @DecimalMin("0.01") BigDecimal monto){}
    public record TransferenciaRequest(@NotNull Long destinoId,@NotNull @DecimalMin("0.01") BigDecimal monto){}
    private final DiscoveryClient discovery;
    private final CircuitBreakerFactory<?,?> breakers;
    private final AtomicInteger roundRobin=new AtomicInteger();
    public PagosController(DiscoveryClient discovery,CircuitBreakerFactory<?,?> breakers){this.discovery=discovery;this.breakers=breakers;}
    @PostMapping("/retiros/{id}") public Map<?,?> retirar(@PathVariable long id,@Valid @RequestBody Monto r){
        return ejecutar("/api/cuentas/"+id+"/debitar",r);
    }
    @PostMapping("/depositos/{id}") public Map<?,?> depositar(@PathVariable long id,@Valid @RequestBody Monto r){
        return ejecutar("/api/cuentas/"+id+"/acreditar",r);
    }
    @PostMapping("/transferencias/{id}") public Map<?,?> transferir(@PathVariable long id,@Valid @RequestBody TransferenciaRequest r){
        return ejecutar("/api/cuentas/"+id+"/transferir",r);
    }
    private Map<?,?> ejecutar(String path,Object body){
        String token=token();
        return breakers.create("cuentasService").run(()->{
          List<org.springframework.cloud.client.ServiceInstance> instances=discovery.getInstances("bancoxyz-cuentas-service");
          if(instances.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Servicio de cuentas no disponible");
          String uri=instances.get(Math.floorMod(roundRobin.getAndIncrement(),instances.size())).getUri().toString();
          try {
            return RestClient.builder().baseUrl(uri).defaultHeaders(h->h.setBearerAuth(token)).build()
              .post().uri(path).body(body).retrieve().body(Map.class);
          }catch(RestClientResponseException ex){
            if(ex.getStatusCode().value()==404)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cuenta no encontrada");
            if(ex.getStatusCode().value()==409)throw new ResponseStatusException(HttpStatus.CONFLICT,"Operación rechazada por saldo o regla bancaria");
            if(ex.getStatusCode().value()==400)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Operación inválida");
            throw ex;
          }
        }, error -> {
          if(error instanceof ResponseStatusException r)throw r;
          throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Operación bancaria temporalmente no disponible");
        });
    }
    private String token(){
       Authentication a=SecurityContextHolder.getContext().getAuthentication();
       if(a instanceof JwtAuthenticationToken jwt)return jwt.getToken().getTokenValue();
       throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Token no disponible");
    }
}
