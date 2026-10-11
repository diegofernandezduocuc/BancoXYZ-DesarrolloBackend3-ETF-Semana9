package cl.duoc.bancoxyz.mobile.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BackendMovimiento(Long cuentaId, LocalDate fecha, String tipo, BigDecimal monto, String descripcion) {
}
