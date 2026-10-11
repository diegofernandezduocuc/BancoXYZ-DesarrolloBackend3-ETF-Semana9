$ErrorActionPreference = 'Stop'
$base = 'https://raw.githubusercontent.com/KariVillagran/fin_legacy_data/main/data/semana_3'
$destination = Join-Path $PSScriptRoot '..\batch-service\src\main\resources\data\semana_3'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$files = @('movimientos_financieros_diarios.csv','intereses_trimestrales.csv','estados_financieros_anuales.csv')
foreach ($file in $files) {
  $target = Join-Path $destination $file
  Invoke-WebRequest -Uri "$base/$file" -OutFile $target -ErrorAction Stop
  if ((Get-Item $target).Length -lt 1000) { throw "Datos CSV incompletos: $file" }
  Write-Host "$file - $((Get-FileHash $target -Algorithm SHA256).Hash)"
}
