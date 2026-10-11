package cl.duoc.bancoxyz.atm.client;

import java.math.BigDecimal;

public record BackendCuenta(Long id, String titular, BigDecimal saldo, Integer edad, String tipo) {
}
