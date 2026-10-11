[CmdletBinding()]
param(
    [ValidateSet('Verificar', 'Recuperar', 'Nuevo')]
    [string]$Accion = 'Verificar',
    [string]$ProyectoDocker,
    [switch]$ConfirmarProyectoNuevo
)

$ErrorActionPreference = 'Stop'
$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$archivo = Join-Path $raiz '.env'
$claves = @('COMPOSE_PROJECT_NAME', 'OAUTH_CLIENT_ID', 'OAUTH_CLIENT_SECRET', 'DB_PASSWORD', 'MYSQL_ROOT_PASSWORD')

function Obtener-Configuracion {
    if (-not (Test-Path -LiteralPath $archivo)) {
        throw 'Falta .env. Recupera el entorno Docker existente o crea uno nuevo con el script.'
    }
    $config = @{}
    foreach ($linea in (Get-Content -LiteralPath $archivo -Encoding UTF8)) {
        if ($linea -match '^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*)$') {
            $config[$Matches[1]] = $Matches[2].Trim()
        }
    }
    foreach ($clave in $claves) {
        if (-not $config.ContainsKey($clave) -or [string]::IsNullOrWhiteSpace($config[$clave]) -or
            $config[$clave] -match '^CAMBIAR_') {
            throw "Falta un valor valido para $clave en .env."
        }
        $actual = [Environment]::GetEnvironmentVariable($clave, 'Process')
        if ($null -ne $actual -and $actual -ne '' -and $actual -ne $config[$clave]) {
            throw "La variable de sesion $clave contradice .env. Abre una terminal nueva para evitar credenciales diferentes."
        }
    }
    Write-Host "Configuracion local lista para el proyecto '$($config['COMPOSE_PROJECT_NAME'])'. No se mostraron secretos."
    Write-Host 'Usa esta misma carpeta y no cambies el nombre del proyecto Compose entre sesiones.'
}

function Obtener-IdContenedor([string]$proyecto, [string]$servicio) {
    $salida = @(& docker ps -a --filter "label=com.docker.compose.project=$proyecto" --filter "label=com.docker.compose.service=$servicio" --format '{{.ID}}')
    if ($LASTEXITCODE -ne 0) { throw "No fue posible consultar Docker para $servicio." }
    $ids = @($salida | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($ids.Count -ne 1) {
        throw "Se esperaban 1 contenedor de $servicio en '$proyecto'; se encontraron $($ids.Count). No se modifico nada."
    }
    return $ids[0].Trim()
}

function Obtener-VariablesContenedor([string]$id) {
    $json = & docker inspect --format '{{json .Config.Env}}' $id
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo inspeccionar el contenedor.' }
    $mapa = @{}
    foreach ($entrada in @(ConvertFrom-Json -InputObject $json)) {
        $indice = $entrada.IndexOf('=')
        if ($indice -gt 0) { $mapa[$entrada.Substring(0, $indice)] = $entrada.Substring($indice + 1) }
    }
    return $mapa
}

function Guardar-Configuracion([hashtable]$datos) {
    if (Test-Path -LiteralPath $archivo) {
        throw 'Ya existe .env; por seguridad no se sobrescribe ni se reemplazan contrasenas.'
    }
    foreach ($clave in $claves) {
        if (-not $datos.ContainsKey($clave) -or [string]::IsNullOrEmpty($datos[$clave])) {
            throw "Falta $clave; no se creo .env."
        }
        if ($datos[$clave] -notmatch '^[a-zA-Z0-9._-]+$') {
            throw "El valor de $clave necesita escape especial en .env. No se guardo parcialmente."
        }
    }
    $lineas = foreach ($clave in $claves) { '{0}={1}' -f $clave, $datos[$clave] }
    $texto = ($lineas -join "`r`n") + "`r`n"
    [System.IO.File]::WriteAllText($archivo, $texto, [System.Text.UTF8Encoding]::new($false))
    Write-Host '.env creado localmente (ignorado por Git). No se imprimieron contrasenas.'
}

function Generar-Clave {
    $bytes = New-Object byte[] 24
    $generador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generador.GetBytes($bytes) } finally { $generador.Dispose() }
    return ([System.BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
}

Push-Location $raiz
try {
    switch ($Accion) {
        'Verificar' {
            Obtener-Configuracion
        }
        'Recuperar' {
            if ($ProyectoDocker -notmatch '^[a-z0-9][a-z0-9_-]*$') { throw 'Nombre de proyecto Docker no valido.' }
            if (Test-Path -LiteralPath $archivo) { throw 'Ya existe .env; no se recuperan ni sobrescriben credenciales.' }
            $mysqlId = Obtener-IdContenedor $ProyectoDocker 'mysql'
            $authId = Obtener-IdContenedor $ProyectoDocker 'auth-server'
            $mysql = Obtener-VariablesContenedor $mysqlId
            $auth = Obtener-VariablesContenedor $authId
            $datos = @{
                COMPOSE_PROJECT_NAME = $ProyectoDocker
                OAUTH_CLIENT_ID = $auth['OAUTH_CLIENT_ID']
                OAUTH_CLIENT_SECRET = $auth['OAUTH_CLIENT_SECRET']
                DB_PASSWORD = $mysql['MYSQL_PASSWORD']
                MYSQL_ROOT_PASSWORD = $mysql['MYSQL_ROOT_PASSWORD']
            }
            if ((& docker inspect --format '{{.State.Running}}' $mysqlId) -ne 'true') {
                throw 'El contenedor MySQL no esta activo; no se puede comprobar la clave almacenada. No se creo .env.'
            }
            $resultado = & docker exec --env "MYSQL_PWD=$($datos['DB_PASSWORD'])" $mysqlId mysql --protocol=TCP -h 127.0.0.1 -ubancoxyz -N -s -e 'SELECT 1;' 2>$null
            if ($LASTEXITCODE -ne 0 -or $resultado -notcontains '1') {
                throw 'La contrasena del contenedor NO coincide con la cuenta de MySQL. No se creo .env ni se modificaron datos.'
            }
            Guardar-Configuracion $datos
            Obtener-Configuracion
        }
        'Nuevo' {
            if (-not $ConfirmarProyectoNuevo) { throw 'Para preparar una base nueva, agrega -ConfirmarProyectoNuevo. No se tocan volúmenes existentes.' }
            if (Test-Path -LiteralPath $archivo) { throw 'Ya existe .env; no se sobrescribe.' }
            $nombre = 'bancoxyz_etf_s9_' + (Get-Date -Format 'yyyyMMddHHmmss')
            $datos = @{
                COMPOSE_PROJECT_NAME = $nombre
                OAUTH_CLIENT_ID = 'bancoxyz-client'
                OAUTH_CLIENT_SECRET = Generar-Clave
                DB_PASSWORD = Generar-Clave
                MYSQL_ROOT_PASSWORD = Generar-Clave
            }
            Guardar-Configuracion $datos
            Obtener-Configuracion
            Write-Host 'Proyecto nuevo: al ejecutar Compose se crearan volumenes independientes. No elimina los anteriores.'
        }
    }
}
finally {
    Pop-Location
}
