# Despliegue controlado en AWS

## Alcance

Este documento describe un procedimiento propuesto. El despliegue AWS se omite en esta entrega por decisión del autor y no constituye un criterio probado.
El despliegue de esta ETF se plantea sobre una instancia EC2 Linux de AWS Academy Learner Lab o una cuenta AWS autorizada. La disponibilidad de EC2, límites de recursos, créditos y permisos dependen del laboratorio. Este proyecto no incluye una infraestructura cloud ya desplegada.

## Preparación
1. Activar el laboratorio institucional de AWS Academy y comprobar límites de tiempo y créditos antes de crear recursos.
2. Crear instancia EC2 compatible con Docker (dimensionada según la memoria realmente observada; la propuesta local estima 4 vCPU y 8 GiB como punto de partida para los servicios de dominio con réplicas, sujeto a límites del laboratorio), y un volumen EBS suficientemente amplio. Evitar instancias pequeñas: Kafka y las aplicaciones Java consumen memoria significativa. La capacidad inicial y el tamaño de EC2 deben confirmarse antes de crear recursos.
3. Configurar un grupo de seguridad que permita administración SSH solamente desde la IP autorizada. Publicar hacia el exterior exclusivamente HTTPS (443 o 8443 temporalmente), nunca MySQL, Kafka, Eureka ni el servidor OAuth HTTP de pruebas.
4. Instalar Docker Engine y el plugin Compose siguiendo la documentación oficial de la distribución Linux elegida: https://docs.docker.com/engine/install/ .
5. Clonar el repositorio GitHub de la entrega en la instancia y sincronizar los CSV oficiales usando `bash scripts/descargar_datos_etf.sh` (el archivo no tiene permiso ejecutable en Git).
6. Crear `.env` con secretos fuertes mediante un mecanismo seguro; evitar ponerlos en repositorios o registros de terminal. En despliegues sostenidos usar AWS Secrets Manager y un proveedor externo de identidad.
7. Revisar la resolución DNS, el emisor OAuth2/JWT y las URLs de servicio. El perfil actual usa URI internas Docker y CA local de Caddy, válido como demostración, no como configuración pública definitiva.
8. Crear DNS propio para el gateway y configurar un certificado TLS de autoridad confiable (ACM con balanceador de aplicaciones o Caddy con dominio público). Exponer solo 443; establecer política de cifrado interno y seguridad de red según el entorno.
9. Ejecutar `docker compose config --quiet`, construir imágenes y levantar los contenedores; verificar `docker compose ps` y registros por servicio. Revisar autenticación, transferencia, eventos Kafka, persistencia, Batch y tolerancia a fallos con pruebas reproducibles.
10. Capturar únicamente resultados finales validados; documentar región, tipo de instancia, restricciones del laboratorio y costos cuando se realice una prueba cloud real.

## Kafka en AWS
Kafka escucha en `kafka:19092` para clientes de la misma red Docker, no en la IP pública. Verificar la producción y el consumo con eventos de una operación bancaria real y consultar el registro del consumidor. Para brokers remotos adicionales se requiere configurar listeners anunciados, autenticación, TLS y reglas de red privadas; no publicar el puerto 9092 al mundo.

## Recuperación y costos
Verificar volúmenes EBS y bases de datos, realizar respaldo antes de operaciones destructivas y revisar reinicio de contenedores. Al terminar, detener la instancia EC2 y los recursos externos del laboratorio. Los volúmenes, IP elásticas y otros servicios pueden seguir teniendo costo según el tipo de cuenta. No se afirma despliegue efectivo hasta registrar una prueba real.

## Prueba de escalabilidad horizontal

Antes de una prueba de escalado, verificar que la versión de Docker Compose admita `!reset` (Compose 2.24 o posterior). El archivo opcional `docker-compose.escalado.yaml` retira los puertos fijos del host para los tres microservicios de dominio. Esto permite poner a prueba sus réplicas dentro de la red privada; los BFF y el gateway mantienen sus puertos de entrada.

```bash
docker compose -f docker-compose.yaml -f docker-compose.escalado.yaml config --quiet
docker compose -f docker-compose.yaml -f docker-compose.escalado.yaml up -d --scale cuentas-service=2 --scale pagos-service=2 --scale clientes-service=2
```

Verificar con `docker compose ps`, consultar Eureka y comprobar el enrutamiento mediante los BFF y los registros de las instancias. La ejecución debe realizarse solo en un entorno de laboratorio aislado y con recursos suficientes; estas instrucciones son un procedimiento pendiente de validación, no evidencia de que AWS o el escalado hayan sido desplegados.
