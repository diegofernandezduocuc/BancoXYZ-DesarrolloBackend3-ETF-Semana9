package cl.duoc.bancoxyz.mobile.model;

import java.math.BigDecimal;

public record MobileCuentaResponse(Long id, String titular, BigDecimal saldo) {
}
