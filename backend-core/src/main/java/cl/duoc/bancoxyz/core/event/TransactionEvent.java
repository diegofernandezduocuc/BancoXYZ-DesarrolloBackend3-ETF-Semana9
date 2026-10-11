package cl.duoc.bancoxyz.core.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionEvent(
        UUID eventId,
        String tipo,
        Long cuentaId,
        BigDecimal monto,
        BigDecimal saldoResultante,
        LocalDateTime fechaHora
) {}
