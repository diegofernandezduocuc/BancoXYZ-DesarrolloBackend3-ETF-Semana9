package cl.duoc.bancoxyzbatch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionProcesada(long sourceId, LocalDate fecha, BigDecimal monto, String tipo,
                                   BigDecimal montoFirmado) { }
