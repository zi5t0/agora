# Angular en el hito H1

H1 crea la aplicación Angular en `frontend/`. Usar Angular 22 con Node 24
compatible (24.15.0 como mínimo para esta rama), TypeScript estricto, componentes
standalone, Angular Router y Vitest. Fijar versiones exactas al implementar,
versionar `package-lock.json` y usar `npm ci` en CI. [Compatibilidad de Angular](https://angular.dev/reference/versions)

## Primera entrega

- Usar `src/app/features/health/` para el componente y el cliente HTTP de salud.
  Incorporar otros paquetes por funcionalidad conforme se implementen catálogo,
  carrito, pedidos y administración de vendedores.
- Una pantalla inicial consulta `GET /actuator/health` mediante `HttpClient` y
  una URL relativa. Mostrar carga durante la consulta, disponible para HTTP 200
  con `status: UP` y error con botón de reintento ante fallo de red, timeout,
  respuesta HTTP de error o cuerpo inesperado. Usar texto visible para el estado
  y permitir operar el botón con teclado.
- Configurar el proxy de Angular CLI para reenviar la ruta exacta de salud al
  backend local en `http://localhost:8080`. En el entorno integrado, servir la
  SPA y enrutar esa petición al backend en el mismo origen. Las rutas de negocio
  se incorporarán bajo `/api/v1` conforme estén disponibles. [Proxy de Angular CLI](https://angular.dev/tools/cli/serve)
- Usar el [contrato OpenAPI](api/openapi.yaml) como referencia. Mantener el
  cliente de esta pantalla acotado a la consulta de salud. Incorporar sesión
  de servidor y protección CSRF para escrituras cuando llegue H3.

El [diseño de Compose](compose.md) documenta el enrutamiento integrado y la
incorporación de `/admin` en H6.

## Aceptación

1. Desde `frontend/`, ejecutar `npm ci`, `npm run test:ci` y `npm run build`.
   Configurar CI para ejecutar esos comandos y conservar el informe de pruebas.
2. Cubrir con pruebas la carga, el éxito, el error y el reintento. Verificar que
   el cliente usa la ruta relativa indicada y muestra un error si el cuerpo
   recibido contradice el contrato.
3. Arrancar Spring Boot y Angular en terminales separadas. Abrir la pantalla y
   comprobar que indica disponible. Detener Spring Boot, recargar y comprobar
   que se muestra error. Volver a arrancarlo y verificar la recuperación con
   el botón de reintento.
