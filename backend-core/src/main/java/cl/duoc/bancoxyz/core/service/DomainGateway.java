package cl.duoc.bancoxyz.core.service;

import cl.duoc.bancoxyz.core.exception.BusinessRuleException;
import cl.duoc.bancoxyz.core.exception.ResourceNotFoundException;
import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.Movimiento;
import cl.duoc.bancoxyz.core.model.RetiroResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DomainGateway {
    private final DiscoveryClient discovery;
    private final InternalBankTokenClient tokenClient;
    private final AtomicInteger next=new AtomicInteger();
    public DomainGateway(DiscoveryClient discovery, InternalBankTokenClient tokenClient){this.discovery=discovery;this.tokenClient=tokenClient;}

    public List<Cuenta> cuentas(){return client("bancoxyz-cuentas-service", "bank.read").get().uri("/api/cuentas")
       .retrieve().body(new ParameterizedTypeReference<>(){});}
    public Cuenta cuenta(long id){return client("bancoxyz-cuentas-service", "bank.read").get().uri("/api/cuentas/{id}",id)
       .retrieve().onStatus(s->s.value()==404,(req,res)->{throw new ResourceNotFoundException("Cuenta no encontrada: "+id);})
       .body(Cuenta.class);}
    public List<Movimiento> movimientos(long id,int limite){return client("bancoxyz-cuentas-service", "bank.read").get()
       .uri(b->b.path("/api/cuentas/{id}/movimientos").queryParam("limit",limite).build(id))
       .retrieve().body(new ParameterizedTypeReference<>(){});}
    public RetiroResponse retiro(long id,BigDecimal monto){
        try{return client("bancoxyz-pagos-service", "bank.write").post().uri("/api/pagos/retiros/{id}",id)
          .body(Map.of("monto",monto)).retrieve().body(RetiroResponse.class);
        }catch(RestClientResponseException ex){
          if(ex.getStatusCode().value()==404) throw new ResourceNotFoundException("Cuenta no encontrada: "+id);
          if(ex.getStatusCode().value()==409) throw new BusinessRuleException("Saldo insuficiente para realizar el retiro");
          if(ex.getStatusCode().value()==400) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Monto de retiro inválido");
          throw ex;
        }
    }
    private RestClient client(String service, String scope){
        List<ServiceInstance> instances=discovery.getInstances(service);
        if(instances.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Servicio no disponible: "+service);
        String uri=instances.get(Math.floorMod(next.getAndIncrement(),instances.size())).getUri().toString();
        String token=tokenClient.token(scope);
        return RestClient.builder().baseUrl(uri).defaultHeaders(h->h.setBearerAuth(token)).build();
    }
}
