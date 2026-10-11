package cl.duoc.bancoxyzbatch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoAnual(String sourceHash, long cuentaId, LocalDate fecha, String transaccion,
                              BigDecimal monto, String descripcion, BigDecimal montoFirmado) { }
