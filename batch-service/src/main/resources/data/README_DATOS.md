Los tres CSV de `semana_3` corresponden a la fuente oficial
https://github.com/KariVillagran/fin_legacy_data/tree/main/data/semana_3.
El 11 de octubre de 2026 se compararon sus bytes con la fuente remota y coincidieron:

| Archivo | SHA-256 |
|---|---|
| movimientos_financieros_diarios.csv | F531FA330F1DC0E0B3511EA8B701DC62A63D98F0A92304B99F8E2CA6C29EF4B8 |
| intereses_trimestrales.csv | C6AA08454600D1C355D6306D8D8941A1E9C0A1D2F6C275177AA0735DB255D23A |
| estados_financieros_anuales.csv | CF62D3AD676CB739DFC5CCAE26612F3A107EBF7560C31B7AB2F1044592AAE7E0 |

Los scripts de descarga permiten actualizar los archivos; una nueva descarga
puede cambiar los datos y exigir ajustar las expectativas de las pruebas.
El nombre `intereses_trimestrales.csv` no demuestra equivalencia con el requisito
de cálculo mensual ni con las reglas COBOL. Esa comparación sigue pendiente.
