package cl.duoc.bancoxyz.atm.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BackendRetiroResponse(
        Long cuentaId,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoActual,
        LocalDateTime fechaHora,
        String estado
) {
}
