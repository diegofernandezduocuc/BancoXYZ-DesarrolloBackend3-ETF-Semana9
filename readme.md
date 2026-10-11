# Banco XYZ — Evaluación Final Transversal, Desarrollo Backend III

## Proyecto
Modernización de los procesos legacy mediante Spring Batch, microservicios Spring Cloud, canales BFF, seguridad OAuth 2.0/JWT, resiliencia con Resilience4j y mensajería Apache Kafka. La solución es una implementación académica para desarrollo local y despliegue controlado mediante Docker Compose.

## Arquitectura

| Componente | Responsabilidad |
|---|---|
| `batch-service` | Migración de los tres conjuntos CSV oficiales con Spring Batch, particiones y reintentos |
| `cuentas-service` | Apertura, consultas, movimientos, cierre y saldos transaccionales (MySQL) |
| `pagos-service` | Retiros, depósitos y transferencias, desacoplados de Cuentas |
| `clientes-service` | Consulta y mantenimiento de perfiles de clientes |
| `backend-core` | Fachada compatible con los BFF de las entregas anteriores |
| `bff-web`, `bff-mobile`, `bff-atm` | Consultas especializadas y operaciones del canal correspondiente |
| `config-server`, `discovery-server` | Configuración centralizada y descubrimiento Eureka |
| `auth-server` | Servidor OAuth 2.0 y emisión de JWT |
| `audit-service` | Consumo de eventos bancarios desde Kafka |
| `mysql`, `kafka`, `tls-gateway` | Persistencia, broker de eventos y entrada HTTPS |

Las escrituras de Cuentas se realizan en transacciones SQL. Las transferencias bloquean ambas cuentas en orden determinista y se confirman atómicamente. La publicación asíncrona emplea una tabla outbox persistente, con reintento y eventId. Los consumidores deben tolerar entregas repetidas.

## Tecnologías
Java 17, Spring Boot 3.4.12, Spring Cloud 2024.0.3, Maven, Spring Batch, Spring Security, OAuth2, Resilience4j, Kafka, MySQL 8.4, Docker Compose y Caddy (HTTPS local).

## Código y ejecución

- `instrucciones.md`: instalación, ejecución y pruebas de cada componente.
- `despliegue.md`: procedimiento propuesto para AWS y consideraciones de seguridad; no se ejecutó el despliegue.
- `docs/arquitectura.md`: componentes e interacciones.
- `scripts/descargar_datos_etf.ps1`: sincronización de los tres CSV de la fuente oficial.

El directorio `batch-service/src/main/resources/data/semana_3` contiene los CSV oficiales obtenidos de `https://github.com/KariVillagran/fin_legacy_data`. El script `scripts/descargar_datos_etf.ps1` permite sincronizarlos nuevamente. La configuración de laboratorio no representa infraestructura bancaria productiva.

## Repositorio de referencia para datos ETF
https://github.com/KariVillagran/fin_legacy_data

## Repositorio de código de la entrega S9
https://github.com/diegofernandezduocuc/BancoXYZ-DesarrolloBackend3-ETF-Semana9

La dirección anterior corresponde al repositorio de código fuente de esta entrega. Los archivos CSV de referencia provienen del repositorio externo indicado en la sección precedente.

## Alcance de la entrega

Se incluye el informe técnico con 15 evidencias. El despliegue en AWS se presenta como propuesta documental, sin ejecución ni validación en la nube. La presentación en video no forma parte de este paquete. No se dispone del programa COBOL original ni de mediciones comparables para afirmar equivalencia financiera o mejoras de rendimiento.
