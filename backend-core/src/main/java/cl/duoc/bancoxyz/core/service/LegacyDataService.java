package cl.duoc.bancoxyz.core.service;

import cl.duoc.bancoxyz.core.event.TransactionEvent;
import cl.duoc.bancoxyz.core.event.TransactionEventPublisher;
import cl.duoc.bancoxyz.core.exception.BusinessRuleException;
import cl.duoc.bancoxyz.core.exception.ResourceNotFoundException;
import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.Movimiento;
import cl.duoc.bancoxyz.core.model.ResumenTransacciones;
import cl.duoc.bancoxyz.core.model.RetiroResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LegacyDataService {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    private final Map<Long, AccountProfile> profiles = new LinkedHashMap<>();
    private final Map<Long, BigDecimal> balances = new ConcurrentHashMap<>();
    private final Map<Long, List<Movimiento>> movements = new ConcurrentHashMap<>();
    private ResumenTransacciones transactionSummary;
    private final TransactionEventPublisher eventPublisher;
    private DomainGateway gateway;

    @Autowired(required=false)
    public void setDomainGateway(DomainGateway gateway) {
        this.gateway=gateway;
    }

    public LegacyDataService(TransactionEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    void loadData() {
        loadAccounts();
        loadMovements();
        transactionSummary = loadTransactionSummary();
    }

    public List<Cuenta> getCuentas() {
        if (gateway != null) return gateway.cuentas();
        return profiles.keySet().stream()
                .sorted()
                .map(this::toCuenta)
                .toList();
    }

    public Cuenta getCuenta(Long id) {
        if (gateway != null) return gateway.cuenta(id);
        if (!profiles.containsKey(id)) {
            throw new ResourceNotFoundException("Cuenta no encontrada: " + id);
        }
        return toCuenta(id);
    }

    public List<Movimiento> getMovimientos(Long id, int limit) {
        if (gateway != null) return gateway.movimientos(id, limit);
        getCuenta(id);
        int effectiveLimit = Math.max(1, Math.min(limit, 50));
        return movements.getOrDefault(id, List.of()).stream()
                .sorted(Comparator.comparing(Movimiento::fecha).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    public ResumenTransacciones getTransactionSummary() {
        return transactionSummary;
    }

    public RetiroResponse retirar(Long id, BigDecimal amount) {
        if (gateway != null) return gateway.retiro(id,amount);
        getCuenta(id);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El monto del retiro debe ser mayor que cero");
        }

        final BigDecimal[] previous = new BigDecimal[1];
        final BigDecimal[] current = new BigDecimal[1];

        balances.compute(id, (key, balance) -> {
            if (balance == null) {
                throw new ResourceNotFoundException("Cuenta no encontrada: " + id);
            }
            if (balance.compareTo(amount) < 0) {
                throw new BusinessRuleException("Saldo insuficiente para realizar el retiro");
            }
            previous[0] = balance;
            current[0] = balance.subtract(amount);
            return current[0];
        });

        LocalDateTime operationTime = LocalDateTime.now();
        movements.computeIfAbsent(id, ignored -> new CopyOnWriteArrayList<>())
                .add(new Movimiento(id, operationTime.toLocalDate(), "retiro", amount, "Retiro realizado desde cajero"));

        eventPublisher.publish(new TransactionEvent(
                UUID.randomUUID(),
                "RETIRO_REALIZADO",
                id,
                amount,
                current[0],
                operationTime
        ));

        return new RetiroResponse(
                id,
                amount,
                previous[0],
                current[0],
                operationTime,
                "APROBADO"
        );
    }

    private Cuenta toCuenta(Long id) {
        AccountProfile profile = profiles.get(id);
        return new Cuenta(id, profile.holder(), balances.get(id), profile.age(), profile.type());
    }

    private void loadAccounts() {
        readCsv("data/semana_3/intereses.csv").stream()
                .skip(1)
                .forEach(line -> {
                    String[] values = line.split(",", -1);
                    if (values.length != 5) {
                        return;
                    }
                    try {
                        long id = Long.parseLong(values[0].trim());
                        String holder = values[1].trim();
                        BigDecimal balance = new BigDecimal(values[2].trim());
                        int age = Integer.parseInt(values[3].trim());
                        String type = values[4].trim().toLowerCase(Locale.ROOT);

                        boolean validType = type.equals("ahorro") || type.equals("prestamo") || type.equals("hipoteca");
                        boolean validHolder = !holder.isBlank() && !holder.equalsIgnoreCase("unknown") && !holder.equals("-1");
                        if (id <= 0 || balance.compareTo(BigDecimal.ZERO) < 0 || age < 18 || age > 99 || !validType || !validHolder) {
                            return;
                        }

                        profiles.putIfAbsent(id, new AccountProfile(holder, age, type));
                        balances.putIfAbsent(id, balance);
                    } catch (NumberFormatException ignored) {
                        // Los datos legacy pueden contener registros inválidos; se descartan al cargar.
                    }
                });
    }

    private void loadMovements() {
        readCsv("data/semana_3/cuentas_anuales.csv").stream()
                .skip(1)
                .forEach(line -> {
                    String[] values = line.split(",", -1);
                    if (values.length != 5) {
                        return;
                    }
                    try {
                        long accountId = Long.parseLong(values[0].trim());
                        if (!profiles.containsKey(accountId)) {
                            return;
                        }
                        Optional<LocalDate> date = parseDate(values[1].trim());
                        String type = normalizeMovementType(values[2].trim());
                        BigDecimal amount = new BigDecimal(values[3].trim());
                        String description = values[4].trim();

                        if (date.isEmpty() || type == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                            return;
                        }

                        movements.computeIfAbsent(accountId, ignored -> new CopyOnWriteArrayList<>())
                                .add(new Movimiento(accountId, date.get(), type, amount, description));
                    } catch (NumberFormatException ignored) {
                        // Registro inválido del conjunto legacy.
                    }
                });
    }

    private ResumenTransacciones loadTransactionSummary() {
        long valid = 0;
        long credits = 0;
        long debits = 0;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;

        for (String line : readCsv("data/semana_3/transacciones.csv").stream().skip(1).toList()) {
            String[] values = line.split(",", -1);
            if (values.length != 4) {
                continue;
            }
            try {
                if (parseDate(values[1].trim()).isEmpty()) {
                    continue;
                }
                BigDecimal amount = new BigDecimal(values[2].trim());
                String type = values[3].trim().toLowerCase(Locale.ROOT);
                if (amount.compareTo(BigDecimal.ZERO) <= 0 || (!type.equals("credito") && !type.equals("debito"))) {
                    continue;
                }

                valid++;
                if (type.equals("credito")) {
                    credits++;
                    totalCredits = totalCredits.add(amount);
                } else {
                    debits++;
                    totalDebits = totalDebits.add(amount);
                }
            } catch (NumberFormatException ignored) {
                // Registro inválido del conjunto legacy.
            }
        }

        return new ResumenTransacciones(
                valid,
                credits,
                debits,
                totalCredits,
                totalDebits,
                totalCredits.subtract(totalDebits)
        );
    }

    private String normalizeMovementType(String rawType) {
        String type = rawType.trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "deposito", "depósito" -> "deposito";
            case "retiro", "compra", "pago" -> type;
            default -> null;
        };
    }

    private Optional<LocalDate> parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
                // Se intenta con el siguiente formato admitido.
            }
        }
        return Optional.empty();
    }

    private List<String> readCsv(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible cargar el archivo " + path, ex);
        }
    }

    private record AccountProfile(String holder, Integer age, String type) {
    }
}
