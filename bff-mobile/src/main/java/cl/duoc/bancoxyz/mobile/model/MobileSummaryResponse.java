package cl.duoc.bancoxyz.mobile.model;

import java.math.BigDecimal;
import java.util.List;

public record MobileSummaryResponse(
        Long cuentaId,
        String titular,
        BigDecimal saldo,
        List<MobileMovimientoResponse> ultimosMovimientos
) {
}
