# Instalación, ejecución y verificación

## Requisitos
Windows 11 con Docker Desktop y Docker Compose; conexión a Internet para imágenes, dependencias y CSV; PowerShell. Java 17 y Maven Wrapper se utilizan para ejecutar pruebas sin contenedores. Los servicios están diseñados para levantar juntos en Docker.

## Inicialización
Desde la raíz del proyecto. Los CSV oficiales ya están incluidos; la descarga siguiente es opcional y reemplaza los archivos con la versión remota disponible:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\descargar_datos_etf.ps1
```

El archivo `.env` local conserva el nombre del proyecto Compose y sus credenciales entre ejecuciones; está excluido de Git y del contexto de construcción Docker. Para iniciar un entorno nuevo, únicamente si no se requiere conservar una base existente, ejecutar **una sola vez**:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\entorno_local.ps1 -Accion Nuevo -ConfirmarProyectoNuevo
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\entorno_local.ps1 -Accion Verificar
```

Si ya existe `.env`, ejecutar únicamente `Verificar` para reutilizar su configuración. El nombre Compose depende del entorno y debe coincidir con `COMPOSE_PROJECT_NAME` en `.env`. El modo `Nuevo` genera un nombre con fecha y tres secretos distintos; no utiliza `.env.example`. La alternativa manual es copiar `.env.example` a `.env`, asignar un nombre exclusivo y reemplazar sus tres valores `CAMBIAR_`. Para los ejemplos siguientes, indicar explícitamente el nombre de la instalación independiente:

```powershell
$proyectoEsperado = Read-Host 'Nombre del proyecto Compose verificado y autorizado en este entorno'
if ([string]::IsNullOrWhiteSpace($proyectoEsperado)) { throw 'Falta el nombre del proyecto Compose' }
```

Si falta `.env`, la recuperación del entorno existente requiere autorización previa y comprobación del nombre:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\entorno_local.ps1 -Accion Recuperar -ProyectoDocker $proyectoEsperado
```

La recuperación requiere el contenedor MySQL activo y verifica el acceso sin cambiar usuarios ni volúmenes. Nunca ejecutar `Nuevo` sobre una base preexistente ni sobrescribir `.env` para cambiar credenciales. Las variables exportadas en la sesión prevalecen sobre `.env`; el modo `Verificar` detecta discrepancias. No publicar `.env`.

**Precaución al cambiar de carpeta de proyecto:** comprobar la identidad Compose sin imprimir secretos, después de `Verificar`:

```powershell
$configCompose = docker compose config --format json | ConvertFrom-Json
if ($configCompose.name -ne $proyectoEsperado) { throw 'Proyecto Compose fuera de alcance' }
$configCompose.name
```

No imprimir `$configCompose` completo. Las comprobaciones HTTP de `localhost` validan el stack activo, no necesariamente el código actual de esta carpeta. Un nombre Compose nuevo separa redes y volúmenes, pero no libera los puertos del host ni cambia las etiquetas de las imágenes. Si otra instalación usa esos puertos, ejecutar en otro equipo o preparar una configuración aislada con puertos y etiquetas propios antes de construir o iniciar.

Construir y ejecutar solamente en un entorno nuevo autorizado. Sobre el stack activo estos comandos pueden recrear contenedores y requieren aprobación:

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

La primera construcción puede tardar por las dependencias Maven. Puertos de consulta local: 8761 Eureka, 8888 Config Server, 9000 OAuth2, 8080 fachada, 8081/8082/8083 BFF, 8084 auditoría, 8085 cuentas, 8086 pagos, 8087 clientes, 3307 MySQL. El gateway HTTPS se publica en `https://localhost:8443` mediante una CA local de Caddy. El certificado de laboratorio no estará confiado automáticamente por el navegador.

## Seguridad y endpoints
Obtener JWT mediante flujo client_credentials:

```powershell
$env:OAUTH_CLIENT_ID = 'bancoxyz-client'
$env:OAUTH_CLIENT_SECRET = Read-Host 'Secreto OAuth local'
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$($env:OAUTH_CLIENT_ID):$($env:OAUTH_CLIENT_SECRET)"))
$token = (Invoke-RestMethod -Method Post -Uri 'http://localhost:9000/oauth2/token' -Headers @{Authorization="Basic $basic"} -ContentType 'application/x-www-form-urlencoded' -Body 'grant_type=client_credentials&scope=bank.read%20bank.write%20web.read%20mobile.read%20atm.read%20atm.write%20audit.read').access_token
$headers = @{Authorization="Bearer $token"}
Invoke-RestMethod 'http://localhost:8081/api/web/cuentas' -Headers $headers
Invoke-RestMethod 'http://localhost:8082/api/mobile/cuentas' -Headers $headers
Invoke-RestMethod 'http://localhost:8083/api/atm/cuentas/106/saldo' -Headers $headers
Invoke-RestMethod 'http://localhost:8085/api/cuentas' -Headers $headers
Invoke-RestMethod 'http://localhost:8087/api/clientes' -Headers $headers
```

Verificar errores negativos: llamadas sin token deben devolver `401`; retiros con saldo insuficiente deben devolver `409`. Para retiro en cajero se usa `POST /api/atm/cuentas/{id}/retiros` con el monto válido, según el contrato existente de Semana 8. La persistencia se comprueba consultando saldo, reiniciando `cuentas-service` y consultando nuevamente; no deben cambiar los valores.

### Kafka y Resilience4j
Verificar consumo en auditoría (`GET http://localhost:8084/api/audit/eventos` con JWT autorizado) después de una operación aprobada y del intervalo de publicación outbox. Para probar fallback, detener temporalmente un servicio de dominio, verificar respuesta 503 controlada y reiniciarlo. No hacer estas pruebas en infraestructura compartida.

## Spring Batch
Para ejecutar los tres jobs sobre el conjunto oficial sincronizado:

```powershell
docker compose --profile batch run --build --rm batch-service --job=todos
```

Jobs disponibles: `transacciones`, `intereses`, `anuales` y `todos`.

Las tasas de la implementación son `INTERES_AHORRO=0.005`, `INTERES_PRESTAMO=0.015` e `INTERES_HIPOTECA=0.008` por defecto. Se aplican una vez al saldo aceptado y el interés se redondea a dos decimales con `HALF_UP`; no se deduce una periodicidad contractual a partir del nombre del CSV. La configuración se encuentra en `batch-service/src/main/resources/application.properties`.

Para reiniciar una ejecución que haya fallado, usar **el mismo** identificador `--run-id` (por ejemplo, `--job=transacciones --run-id=lote-2026-10`). Los reintentos automáticos del proceso usan los mismos parámetros de JobInstance y no reinician desde cero las particiones completadas. Una ejecución ya completada no puede repetirse con el mismo identificador: para procesar de nuevo los CSV se usa otro `--run-id` o se omite el argumento para obtener uno nuevo. Comprobar el estado de los pasos y la integridad de tablas antes y después de un fallo controlado; no interrumpir la base compartida durante las pruebas. Las tablas `transacciones_procesadas`, `intereses_calculados` y `movimientos_anuales` se crean en la base `bancoxyz_batch` y permiten consultar resultados. Consultar `batch-service/src/main/resources/schema-business.sql` para el esquema completo. El archivo oficial se denomina `intereses_trimestrales.csv`, mientras que la actividad pide el cálculo de intereses mensuales. Las tasas y el período de cálculo deben explicarse y contrastarse con los criterios oficiales antes de afirmar equivalencia con el proceso COBOL original.

## Pruebas de regresión
Con JDK 17 y Maven Wrapper, desde una copia separada si se desea mantener la entrega sin directorios `target`:

```powershell
.\mvnw.cmd -B -ntp '-Dspring.cloud.config.enabled=false' '-Dspring.cloud.discovery.enabled=false' '-Deureka.client.enabled=false' verify
```

El modo sin conexión añade `-o` y requiere que Maven y todas las dependencias ya estén en la caché local. En una primera instalación se debe mantener el comando sin `-o`.

Para una comprobación local resumida del stack Docker activo, con salida `PASS/FAIL/SKIP` y sin imprimir secretos ni tokens:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\validar_etf_s9.ps1 -ProyectoDocker $proyectoEsperado
```

Las comprobaciones autenticadas sin JWT se registran como `FAIL` al obtener el token y `SKIP` para cada solicitud que no pudo ejecutarse; no se cuentan como aprobadas. Por seguridad, el `POST` de retiro inválido está desactivado por defecto: activarlo con `-ProbarEntradaInvalida` **solamente sobre una base aislada**, porque una validación defectuosa podría aceptar la operación. La prueba de HTTPS desde Windows puede fallar por Schannel aunque la prueba desde Docker pase; revisar ambas por separado, sin ocultar el fallo. Los códigos HTTP en `localhost` pertenecen al stack que actualmente publica esos puertos; confirmar el origen de las imágenes antes de presentarlos como evidencia final.

No se deben presentar como aprobadas pruebas no ejecutadas. Para detener sin borrar volúmenes:

```powershell
docker compose down
```

Eliminar volúmenes con `down -v` borraría las bases de datos; requiere respaldo previo y autorización.
