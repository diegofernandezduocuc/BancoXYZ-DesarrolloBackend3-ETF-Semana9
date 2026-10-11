package cl.duoc.bancoxyz.web.model;

import java.math.BigDecimal;

public record WebCuentaResponse(Long id, String titular, BigDecimal saldo, Integer edad, String tipo) {
}
