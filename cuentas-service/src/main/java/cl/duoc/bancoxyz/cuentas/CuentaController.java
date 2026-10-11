package cl.duoc.bancoxyz.cuentas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {
    private final CuentaRepository repo;
    public CuentaController(CuentaRepository repo){this.repo=repo;}
    public record Monto(@NotNull @DecimalMin("0.01") BigDecimal monto){}
    public record Apertura(@NotBlank String titular,@Min(18) @Max(99) int edad,@NotBlank String tipo,
                           @NotNull @DecimalMin("0.00") BigDecimal saldo){}
    public record Transferir(@NotNull Long destinoId,@NotNull @DecimalMin("0.01") BigDecimal monto){}

    @GetMapping public List<CuentaRepository.Cuenta> cuentas(){return repo.listar();}
    @GetMapping("/{id}") public CuentaRepository.Cuenta cuenta(@PathVariable long id){return repo.obtener(id);}
    @GetMapping("/{id}/movimientos") public List<CuentaRepository.Movimiento> movimientos(@PathVariable long id,
        @RequestParam(defaultValue="10") int limit){return repo.movimientos(id,limit);}
    @PostMapping public ResponseEntity<Map<String,Long>> apertura(@Valid @RequestBody Apertura d) {
        return ResponseEntity.status(201).body(Map.of("id",repo.apertura(d.titular(),d.edad(),d.tipo(),d.saldo())));
    }
    @PostMapping("/{id}/debitar") public CuentaRepository.Resultado debitar(@PathVariable long id,@Valid @RequestBody Monto d){return repo.movimiento(id,d.monto(),true,"retiro");}
    @PostMapping("/{id}/acreditar") public CuentaRepository.Resultado acreditar(@PathVariable long id,@Valid @RequestBody Monto d){return repo.movimiento(id,d.monto(),false,"deposito");}
    @PostMapping("/{id}/transferir") public CuentaRepository.Transferencia transferencia(@PathVariable long id,@Valid @RequestBody Transferir d){return repo.transferir(id,d.destinoId(),d.monto());}
    @PostMapping("/{id}/cerrar") public ResponseEntity<Void> cerrar(@PathVariable long id){repo.cerrar(id);return ResponseEntity.noContent().build();}
}
