#!/usr/bin/env bash
set -euo pipefail
base="https://raw.githubusercontent.com/KariVillagran/fin_legacy_data/main/data/semana_3"
folder="$(cd "$(dirname "$0")/.." && pwd)/batch-service/src/main/resources/data/semana_3"
mkdir -p "$folder"
for file in movimientos_financieros_diarios.csv intereses_trimestrales.csv estados_financieros_anuales.csv; do
  curl -fLsS "$base/$file" -o "$folder/$file"
  test "$(wc -c < "$folder/$file")" -gt 1000
  sha256sum "$folder/$file"
done
