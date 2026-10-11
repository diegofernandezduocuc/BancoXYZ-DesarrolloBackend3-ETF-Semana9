package cl.duoc.bancoxyz.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RetiroResponse(
        Long cuentaId,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoActual,
        LocalDateTime fechaHora,
        String estado
) {
}
