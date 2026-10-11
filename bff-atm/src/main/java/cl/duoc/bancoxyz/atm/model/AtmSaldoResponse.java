package cl.duoc.bancoxyz.atm.model;

import java.math.BigDecimal;

public record AtmSaldoResponse(Long cuentaId, BigDecimal saldoDisponible) {
}
