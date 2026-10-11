[CmdletBinding()]
param(
    [string]$ProyectoDocker,
    [string]$CuentaId = '106',
    [switch]$SinDocker,
    [switch]$ProbarEntradaInvalida
)

$ErrorActionPreference = 'Stop'
if (-not $SinDocker -and [string]::IsNullOrWhiteSpace($ProyectoDocker)) {
    throw 'Indica -ProyectoDocker con el nombre Compose verificado y autorizado en este entorno, o usa -SinDocker.'
}
$resultados = New-Object System.Collections.Generic.List[object]

function Agregar-Resultado {
    param(
        [string]$Nombre,
        [bool]$Ok,
        [string]$Detalle
    )
    $resultados.Add([pscustomobject]@{
        Estado = if ($Ok) { 'PASS' } else { 'FAIL' }
        Prueba = $Nombre
        Detalle = $Detalle
    })
}

function Agregar-Omitido {
    param(
        [string]$Nombre,
        [string]$Detalle
    )
    $resultados.Add([pscustomobject]@{
        Estado = 'SKIP'
        Prueba = $Nombre
        Detalle = $Detalle
    })
}

function Leer-EnvLocal {
    $archivo = Join-Path (Resolve-Path (Join-Path $PSScriptRoot '..')).Path '.env'
    $datos = @{}
    if (-not (Test-Path -LiteralPath $archivo)) { return $datos }
    foreach ($linea in (Get-Content -LiteralPath $archivo -Encoding UTF8)) {
        if ($linea -match '^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*)$') {
            $datos[$Matches[1]] = $Matches[2].Trim()
        }
    }
    return $datos
}

function Obtener-EnvServicioDocker {
    param([string]$Servicio)
    if ($SinDocker) { return @{} }
    $id = (& docker ps --filter "label=com.docker.compose.project=$ProyectoDocker" --filter "label=com.docker.compose.service=$Servicio" --format '{{.ID}}' 2>$null | Select-Object -First 1)
    if ([string]::IsNullOrWhiteSpace($id)) { return @{} }
    $json = & docker inspect --format '{{json .Config.Env}}' $id 2>$null
    $datos = @{}
    foreach ($entrada in @(ConvertFrom-Json -InputObject $json)) {
        $indice = $entrada.IndexOf('=')
        if ($indice -gt 0) {
            $datos[$entrada.Substring(0, $indice)] = $entrada.Substring($indice + 1)
        }
    }
    return $datos
}

function Obtener-IdServicioDocker {
    param([string]$Servicio)
    if ($SinDocker) { return $null }
    return (& docker ps --filter "label=com.docker.compose.project=$ProyectoDocker" --filter "label=com.docker.compose.service=$Servicio" --format '{{.ID}}' 2>$null | Select-Object -First 1)
}

function Invocar-Http {
    param(
        [ValidateSet('GET', 'POST')]
        [string]$Metodo,
        [string]$Url,
        [string]$Token,
        [object]$Body
    )
    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers['Authorization'] = "Bearer $Token"
    }
    $parametros = @{
        Method = $Metodo
        Uri = $Url
        Headers = $headers
        UseBasicParsing = $true
        TimeoutSec = 20
    }
    if ($null -ne $Body) {
        $parametros['ContentType'] = 'application/json'
        $parametros['Body'] = ($Body | ConvertTo-Json -Compress)
    }
    try {
        $respuesta = Invoke-WebRequest @parametros
        return [int]$respuesta.StatusCode
    } catch {
        $status = $_.Exception.Response.StatusCode
        if ($null -ne $status) { return [int]$status }
        return -1
    }
}

function Obtener-Token {
    param([string]$Scope)
    $envLocal = Leer-EnvLocal
    $envAuth = Obtener-EnvServicioDocker 'auth-server'
    if (-not $envLocal['OAUTH_CLIENT_SECRET']) {
        $authId = Obtener-IdServicioDocker 'auth-server'
        if (-not [string]::IsNullOrWhiteSpace($authId)) {
            try {
                $jsonToken = & docker exec --env "REQ_SCOPE=$Scope" $authId sh -c 'curl -s -u "$OAUTH_CLIENT_ID:$OAUTH_CLIENT_SECRET" -d grant_type=client_credentials --data-urlencode "scope=$REQ_SCOPE" http://localhost:9000/oauth2/token' 2>$null
                $respuestaDocker = $jsonToken | ConvertFrom-Json
                if (-not [string]::IsNullOrWhiteSpace($respuestaDocker.access_token)) {
                    return $respuestaDocker.access_token
                }
            } catch {
                # Sigue con el metodo host-side si Docker no puede emitir el token.
            }
        }
    }
    $clientId = if ($envLocal['OAUTH_CLIENT_ID']) { $envLocal['OAUTH_CLIENT_ID'] } elseif ($envAuth['OAUTH_CLIENT_ID']) { $envAuth['OAUTH_CLIENT_ID'] } else { 'bancoxyz-client' }
    $secret = if ($envLocal['OAUTH_CLIENT_SECRET']) { $envLocal['OAUTH_CLIENT_SECRET'] } else { $envAuth['OAUTH_CLIENT_SECRET'] }
    if ([string]::IsNullOrWhiteSpace($secret)) { return $null }
    $basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${clientId}:${secret}"))
    try {
        $respuesta = Invoke-RestMethod -Method Post -Uri 'http://localhost:9000/oauth2/token' `
            -Headers @{ Authorization = "Basic $basic" } `
            -ContentType 'application/x-www-form-urlencoded' `
            -Body @{ grant_type = 'client_credentials'; scope = $Scope } `
            -TimeoutSec 20
        return $respuesta.access_token
    } catch {
        return $null
    }
}

function Verificar-Status {
    param(
        [string]$Nombre,
        [string]$Metodo,
        [string]$Url,
        [int[]]$Esperados,
        [string]$Token,
        [object]$Body
    )
    $codigo = Invocar-Http -Metodo $Metodo -Url $Url -Token $Token -Body $Body
    Agregar-Resultado $Nombre ($Esperados -contains $codigo) "HTTP $codigo esperado: $($Esperados -join '/')"
}

Verificar-Status 'OAuth2 metadata' GET 'http://localhost:9000/.well-known/oauth-authorization-server' 200
Verificar-Status 'OAuth2 JWKS' GET 'http://localhost:9000/oauth2/jwks' 200
Verificar-Status 'BFF Web sin JWT' GET 'http://localhost:8081/api/web/cuentas' 401
Verificar-Status 'BFF Mobile sin JWT' GET 'http://localhost:8082/api/mobile/cuentas' 401
Verificar-Status 'BFF ATM sin JWT' GET "http://localhost:8083/api/atm/cuentas/$CuentaId/saldo" 401

$tokenWeb = Obtener-Token 'web.read'
$tokenMobile = Obtener-Token 'mobile.read'
$tokenAtmRead = Obtener-Token 'atm.read'
$tokenAtmWrite = Obtener-Token 'atm.write'
$tokenBankRead = Obtener-Token 'bank.read'
$tokenAudit = Obtener-Token 'audit.read'

# Registrar siempre la obtencion de cada token; nunca omitir un fallo por ausencia de credenciales.
$tokensPorScope = @{
    'web.read' = $tokenWeb
    'mobile.read' = $tokenMobile
    'atm.read' = $tokenAtmRead
    'atm.write' = $tokenAtmWrite
    'bank.read' = $tokenBankRead
    'audit.read' = $tokenAudit
}
foreach ($scope in @('web.read', 'mobile.read', 'atm.read', 'atm.write', 'bank.read', 'audit.read')) {
    Agregar-Resultado "OAuth2 token $scope" (-not [string]::IsNullOrWhiteSpace($tokensPorScope[$scope])) 'Obtencion de JWT sin imprimir credenciales'
}
if ($tokenWeb) {
    Verificar-Status 'BFF Web con scope correcto' GET 'http://localhost:8081/api/web/cuentas' 200 $tokenWeb
    Verificar-Status 'BFF Mobile con scope incorrecto' GET 'http://localhost:8082/api/mobile/cuentas' 403 $tokenWeb
} else {
    Agregar-Omitido 'BFF Web con scope correcto' 'No se obtuvo JWT web.read'
    Agregar-Omitido 'BFF Mobile con scope incorrecto' 'No se obtuvo JWT web.read'
}
if ($tokenMobile) {
    Verificar-Status 'BFF Mobile con scope correcto' GET 'http://localhost:8082/api/mobile/cuentas' 200 $tokenMobile
} else {
    Agregar-Omitido 'BFF Mobile con scope correcto' 'No se obtuvo JWT mobile.read'
}
if ($tokenAtmRead) {
    Verificar-Status 'BFF ATM saldo con scope correcto' GET "http://localhost:8083/api/atm/cuentas/$CuentaId/saldo" 200 $tokenAtmRead
} else {
    Agregar-Omitido 'BFF ATM saldo con scope correcto' 'No se obtuvo JWT atm.read'
}
if ($tokenAtmWrite -and $ProbarEntradaInvalida) {
    Verificar-Status 'BFF ATM retiro invalido' POST "http://localhost:8083/api/atm/cuentas/$CuentaId/retiros" 400 $tokenAtmWrite @{ monto = -1 }
} else {
    Agregar-Omitido 'BFF ATM retiro invalido' 'POST omitido por seguridad; habilitar solo en una base aislada con -ProbarEntradaInvalida'
}
if ($tokenBankRead) {
    Verificar-Status 'Backend Core cuentas' GET 'http://localhost:8080/api/backend/cuentas' 200 $tokenBankRead
    Verificar-Status 'Cuentas cuenta existente' GET "http://localhost:8085/api/cuentas/$CuentaId" 200 $tokenBankRead
    Verificar-Status 'Clientes listado' GET 'http://localhost:8087/api/clientes' 200 $tokenBankRead
} else {
    Agregar-Omitido 'Backend Core cuentas' 'No se obtuvo JWT bank.read'
    Agregar-Omitido 'Cuentas cuenta existente' 'No se obtuvo JWT bank.read'
    Agregar-Omitido 'Clientes listado' 'No se obtuvo JWT bank.read'
}
if ($tokenAudit) {
    Verificar-Status 'Auditoria listado' GET 'http://localhost:8084/api/audit/eventos' 200 $tokenAudit
} else {
    Agregar-Omitido 'Auditoria listado' 'No se obtuvo JWT audit.read'
}

Verificar-Status 'Config Server BFF Web' GET 'http://localhost:8888/bancoxyz-bff-web/default' 200
Verificar-Status 'Eureka apps' GET 'http://localhost:8761/eureka/apps' 200

if (-not $SinDocker) {
    try {
        $contenedores = @(& docker ps --filter "label=com.docker.compose.project=$ProyectoDocker" --format '{{.Names}}|{{.Status}}')
        $activos = $contenedores.Count
        $noUp = @($contenedores | Where-Object { $_ -notmatch '\|Up ' })
        $saludIncorrecta = @($contenedores | Where-Object { $_ -match '\((unhealthy|health: starting)\)' })
        Agregar-Resultado 'Docker contenedores activos' ($activos -ge 14 -and $noUp.Count -eq 0 -and $saludIncorrecta.Count -eq 0) "$activos contenedores; no Up=$($noUp.Count); salud pendiente/fallida=$($saludIncorrecta.Count); proyecto=$ProyectoDocker"
    } catch {
        Agregar-Resultado 'Docker contenedores activos' $false 'No fue posible consultar Docker'
    }
    try {
        $kafkaId = (& docker ps --filter "label=com.docker.compose.project=$ProyectoDocker" --filter "label=com.docker.compose.service=kafka" --format '{{.ID}}' | Select-Object -First 1)
        $topics = @(& docker exec $kafkaId /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list)
        Agregar-Resultado 'Kafka topic transacciones' ($topics -contains 'bancoxyz.transacciones') "topics=$($topics.Count)"
    } catch {
        Agregar-Resultado 'Kafka topic transacciones' $false 'No fue posible listar topicos'
    }
}

try {
    $codigoHttps = & curl.exe -k -s -o NUL -w '%{http_code}' 'https://localhost:8443/api/web/cuentas'
    Agregar-Resultado 'HTTPS gateway Web sin JWT' ($codigoHttps -eq '401') "HTTP $codigoHttps esperado: 401"
} catch {
    Agregar-Resultado 'HTTPS gateway Web sin JWT' $false 'curl HTTPS fallo en cliente local'
}

if (-not $SinDocker) {
    try {
        $authId = Obtener-IdServicioDocker 'auth-server'
        $codigoHttpsDocker = & docker exec $authId sh -c 'curl -k -s -o /dev/null -w "%{http_code}" --connect-to localhost:443:tls-gateway:443 https://localhost/api/web/cuentas'
        Agregar-Resultado 'HTTPS gateway desde red Docker' ($codigoHttpsDocker -eq '401') "HTTP $codigoHttpsDocker esperado: 401"
    } catch {
        Agregar-Resultado 'HTTPS gateway desde red Docker' $false 'No fue posible probar HTTPS desde Docker'
    }
}

$resultados | Format-Table -AutoSize
$ok = @($resultados | Where-Object Estado -eq 'PASS').Count
$fail = @($resultados | Where-Object Estado -eq 'FAIL').Count
$omitidas = @($resultados | Where-Object Estado -eq 'SKIP').Count
Write-Host "RESUMEN PASS=$ok FAIL=$fail SKIP=$omitidas TOTAL=$($resultados.Count)"
if ($fail -gt 0) { exit 1 }
