package cl.duoc.bancoxyzbatch.model;

import java.math.BigDecimal;

public record InteresCalculado(String sourceHash, long cuentaId, String nombre, BigDecimal saldoInicial,
                               int edad, String tipo, BigDecimal tasa, BigDecimal interes,
                               BigDecimal saldoFinal) { }
