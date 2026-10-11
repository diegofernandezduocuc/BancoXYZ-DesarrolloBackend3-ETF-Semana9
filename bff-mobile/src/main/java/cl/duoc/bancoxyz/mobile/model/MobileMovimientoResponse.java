package cl.duoc.bancoxyz.mobile.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MobileMovimientoResponse(LocalDate fecha, String tipo, BigDecimal monto) {
}
