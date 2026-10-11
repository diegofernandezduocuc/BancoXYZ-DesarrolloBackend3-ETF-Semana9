package cl.duoc.bancoxyz.core.model;

import java.math.BigDecimal;

public record ResumenTransacciones(
        long registrosValidos,
        long creditos,
        long debitos,
        BigDecimal totalCreditos,
        BigDecimal totalDebitos,
        BigDecimal movimientoNeto
) {
}
