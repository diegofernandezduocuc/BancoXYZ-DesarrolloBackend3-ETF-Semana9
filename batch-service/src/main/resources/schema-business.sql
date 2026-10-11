CREATE TABLE IF NOT EXISTS transacciones_procesadas (
    source_id BIGINT PRIMARY KEY,
    fecha DATE NOT NULL,
    monto DECIMAL(15,2) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    monto_firmado DECIMAL(15,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS resumen_transacciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    registros_entrada INT NOT NULL,
    registros_validos INT NOT NULL,
    anomalias_detectadas INT NOT NULL,
    total_creditos DECIMAL(18,2) NOT NULL,
    total_debitos DECIMAL(18,2) NOT NULL,
    movimiento_neto DECIMAL(18,2) NOT NULL,
    duracion_ms BIGINT NOT NULL,
    fecha_ejecucion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intereses_calculados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_hash CHAR(64) NOT NULL UNIQUE,
    cuenta_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    saldo_inicial DECIMAL(18,2) NOT NULL,
    edad INT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    tasa DECIMAL(10,6) NOT NULL,
    interes DECIMAL(18,2) NOT NULL,
    saldo_final DECIMAL(18,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS movimientos_anuales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_hash CHAR(64) NOT NULL UNIQUE,
    cuenta_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    transaccion VARCHAR(20) NOT NULL,
    monto DECIMAL(18,2) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    monto_firmado DECIMAL(18,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS estados_cuenta_anuales (
    cuenta_id BIGINT PRIMARY KEY,
    total_operaciones INT NOT NULL,
    total_ingresos DECIMAL(18,2) NOT NULL,
    total_egresos DECIMAL(18,2) NOT NULL,
    saldo_neto_anual DECIMAL(18,2) NOT NULL,
    fecha_primera DATE NOT NULL,
    fecha_ultima DATE NOT NULL,
    fecha_generacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS registros_rechazados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(80) NOT NULL,
    raw_data VARCHAR(1500) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    fecha_rechazo TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rechazados_job (job_name)
);
