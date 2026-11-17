# Ágora

Base de un e-commerce para tienda propia, marketplace y productos digitales.
El hito H0 entrega una aplicación Spring Boot 4.1.2 con Java 25 y un endpoint
de salud de Actuator.

## Levantar con Docker Compose

Desde la raíz del repositorio, con Docker en ejecución y el complemento
`docker compose` disponible:

```bash
docker compose up --build --wait --wait-timeout 60
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
```

El primer build descarga las imágenes y dependencias, ejecuta las pruebas
de Maven y empaqueta el JAR dentro de Docker. El contenedor se ejecuta con
un usuario sin privilegios y Compose espera a que el endpoint de salud
responda. El puerto solo se publica en la interfaz local. Si el 8080 está
ocupado, define `AGORA_HTTP_PORT=8081` en un archivo `.env` junto a
`compose.yaml` y usa el puerto 8081 en el navegador o en curl.

```bash
docker compose ps
docker compose logs --tail=50 backend
docker compose down
```

El [diseño de integración con Compose](docs/compose.md) describe cómo añadir
los próximos servicios y el panel Angular de administración.

## Verificar y ejecutar la API con Java local

Se necesita JDK 25 y acceso a Maven Central para la primera descarga. Maven
Wrapper gestiona Maven 3.9.16. El backend arranca sin base de datos ni servicios
externos.

Desde la raíz del repositorio:

```bash
cd backend
./mvnw -B clean verify
java -jar target/agora-backend-0.0.1-SNAPSHOT.jar
```

En Windows, usar `mvnw.cmd -B clean verify`. Los informes de integración
quedan en `backend/target/failsafe-reports/`.

Desde otra terminal:

```bash
curl --fail --silent --show-error --include http://localhost:8080/actuator/health
```

La respuesta esperada es HTTP 200 y `{"status":"UP"}`. Para usar otro puerto,
añadir `--server.port=8081` al arranque del JAR y ajustar la URL. Detener la
aplicación con Ctrl+C.

## Documentación

- [Plan y límites de módulos](docs/plan-agora.md)
- [Prompt de implementación por hitos](docs/prompt-implementacion.md)
- [Contrato HTTP actual](docs/api/openapi.yaml)
- [Diseño y aceptación de Angular para H1](docs/frontend-angular.md)
