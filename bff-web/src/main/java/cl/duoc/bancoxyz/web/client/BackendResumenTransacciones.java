package cl.duoc.bancoxyz.web.client;

import java.math.BigDecimal;

public record BackendResumenTransacciones(
        long registrosValidos,
        long creditos,
        long debitos,
        BigDecimal totalCreditos,
        BigDecimal totalDebitos,
        BigDecimal movimientoNeto
) {
}
