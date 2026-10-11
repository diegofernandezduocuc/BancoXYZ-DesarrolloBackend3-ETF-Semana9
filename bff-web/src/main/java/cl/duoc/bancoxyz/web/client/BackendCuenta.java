package cl.duoc.bancoxyz.web.client;

import java.math.BigDecimal;

public record BackendCuenta(Long id, String titular, BigDecimal saldo, Integer edad, String tipo) {
}
