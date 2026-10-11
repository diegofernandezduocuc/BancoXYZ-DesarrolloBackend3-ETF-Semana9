package cl.duoc.bancoxyz.core.model;

import java.math.BigDecimal;

public record Cuenta(
        Long id,
        String titular,
        BigDecimal saldo,
        Integer edad,
        String tipo
) {
}
