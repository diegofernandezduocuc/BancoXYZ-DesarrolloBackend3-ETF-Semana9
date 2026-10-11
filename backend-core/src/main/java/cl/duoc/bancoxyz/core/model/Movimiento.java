package cl.duoc.bancoxyz.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Movimiento(
        Long cuentaId,
        LocalDate fecha,
        String tipo,
        BigDecimal monto,
        String descripcion
) {
}
