CREATE TABLE IF NOT EXISTS auditoria_eventos (
  id VARCHAR(36) PRIMARY KEY,
  tipo VARCHAR(80) NOT NULL,
  cuenta_id BIGINT NOT NULL,
  monto DECIMAL(19,2) NOT NULL,
  saldo_resultante DECIMAL(19,2) NOT NULL,
  fecha_hora DATETIME(6) NOT NULL,
  INDEX ix_auditoria_fecha(fecha_hora)
) ENGINE=InnoDB;
