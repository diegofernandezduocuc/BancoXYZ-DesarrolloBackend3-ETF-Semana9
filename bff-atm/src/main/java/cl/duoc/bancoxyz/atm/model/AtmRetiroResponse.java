package cl.duoc.bancoxyz.atm.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AtmRetiroResponse(
        Long cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoDisponible,
        LocalDateTime fechaHora,
        String estado
) {
}
