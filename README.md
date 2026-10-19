# Cliente Service — Spring Boot

Proyecto académico de API REST para gestionar clientes y regiones.

## Requisitos

- Java 21.
- Acceso a Internet en la primera ejecución para descargar las dependencias de Maven.

## Ejecutar en Windows

Desde la carpeta del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

La configuración predeterminada utiliza H2 en memoria y carga los datos de ejemplo de `src/main/resources/import.sql`. No requiere Docker. Los datos se reinician al detener la aplicación.

- API: http://localhost:8080/api/v1/cliente-service/clientes
- Salud: http://localhost:8081/actuator/health

## Pruebas

```powershell
.\mvnw.cmd test
```

También se incluyen solicitudes de ejemplo en `requests.http` y `src/test/http/`.

## PostgreSQL opcional

Define una contraseña de desarrollo en la misma terminal antes de iniciar Docker y la aplicación:

```powershell
$env:POSTGRES_PASSWORD = 'elige-una-clave-local'
docker compose up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

No publiques contraseñas ni archivos `.env` en el repositorio.
