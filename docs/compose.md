# Integración con Docker Compose

## Backend actual

`compose.yaml` levanta el servicio `backend`. Su Dockerfile compila y ejecuta
`mvnw clean verify` en una etapa con JDK y copia el JAR a otra con JRE.
Las imágenes base se fijan por digest del índice multiplataforma. El directorio
`backend/` es el único contexto del build. El contenedor ejecuta con un
usuario sin privilegios y sistema de archivos de solo lectura, con `/tmp`
escribible para Tomcat.

- En la red de Compose, el endpoint de salud es
  `http://backend:8080/actuator/health`.
- En la máquina anfitriona, la API se publica en `127.0.0.1:8080` por defecto.
  `AGORA_HTTP_PORT` cambia el puerto publicado y mantiene el puerto interno.
- Compose espera a que el endpoint responda correctamente. CI prueba además
  que la petición desde el anfitrión devuelve exactamente el objeto
  `{"status":"UP"}`.

## Incorporación de los siguientes hitos

| Hito | Integración | Verificación |
| --- | --- | --- |
| H1 | Añadir la imagen de Angular, compilada desde `frontend/`. Servir los archivos estáticos y reenviar la ruta de salud a `backend:8080` mediante el servidor HTTP del frontend. | El navegador carga Angular y consulta el backend en su mismo origen. La aplicación muestra error si la API cae y se recupera al reintentar. |
| H2 | Añadir PostgreSQL a la red interna con volumen nombrado, comprobación de salud y configuración del datasource mediante el entorno. Flyway aplica migraciones desde el backend. | Arrancar desde un volumen vacío, agregar datos y verificar persistencia tras recrear los contenedores. |
| H3 | Añadir el proveedor OIDC de desarrollo. Configurar la URL pública que usa el navegador y el enrutamiento interno del backend para validar la sesión. La aplicación mantiene el control de permisos y CSRF. | Iniciar y cerrar sesión y verificar acceso por rol e identidad desde el navegador. |
| H6 | Incorporar la ruta `/admin` al frontend como funcionalidad Angular con carga diferida. Usa rutas relativas hacia la API del backend. El servidor verifica el rol y la propiedad de los recursos en cada operación. | Levantar el entorno con Compose, iniciar sesión como administrador y vendedor, y ejecutar las pruebas de administración e aislamiento del plan. |

Los servicios se enlazan por nombre DNS de Compose. Las imágenes de backend
y frontend se construyen por separado y el navegador se comunica por HTTP
con el mismo origen. Las dependencias internas se comprueban en su frontera
de red. Las credenciales futuras se suministran por entorno o secretos según
el entorno de despliegue.
