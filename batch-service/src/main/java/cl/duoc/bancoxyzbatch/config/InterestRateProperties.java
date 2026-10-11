package cl.duoc.bancoxyzbatch.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "banco.intereses")
public class InterestRateProperties {
    private BigDecimal ahorro = new BigDecimal("0.005");
    private BigDecimal prestamo = new BigDecimal("0.015");
    private BigDecimal hipoteca = new BigDecimal("0.008");

    public BigDecimal getAhorro() { return ahorro; }
    public void setAhorro(BigDecimal ahorro) { this.ahorro = ahorro; }
    public BigDecimal getPrestamo() { return prestamo; }
    public void setPrestamo(BigDecimal prestamo) { this.prestamo = prestamo; }
    public BigDecimal getHipoteca() { return hipoteca; }
    public void setHipoteca(BigDecimal hipoteca) { this.hipoteca = hipoteca; }

    public BigDecimal rateFor(String tipo) {
        return switch (tipo) {
            case "ahorro" -> ahorro;
            case "prestamo" -> prestamo;
            case "hipoteca" -> hipoteca;
            default -> throw new IllegalArgumentException("Tipo de cuenta sin tasa configurada: " + tipo);
        };
    }
}
