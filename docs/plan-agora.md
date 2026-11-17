# Plan de ejecución de Ágora

## Objetivo y alcance

Entregar de forma incremental una tienda que permita buscar productos, mantener un carrito, comprar, pagar y consultar el pedido. El primer hito entregable es una aplicación Spring Boot que arranca desde un clon limpio y responde a una comprobación de salud HTTP. Cada hito tiene una prueba de aceptación y se puede revisar sin esperar a que termine el proyecto completo.

**Alcance confirmado.** El MVP cubre una tienda propia, un marketplace con varios vendedores y productos digitales. Se entrega la planificación en Markdown. H0 será el primer encargo de implementación. La compra de demostración usa un pago simulado y una tarifa de envío fija.

**Decisiones propuestas para concretar el MVP.** Un despliegue admite ambos modelos: el vendedor propio se registra igual que los vendedores externos y una tienda propia funciona con un solo vendedor activo. Los productos físicos y digitales pueden convivir en un pedido. Cada vendedor tiene su propio inventario físico y expide sus artículos; la tarifa fija se cobra una vez por grupo de vendedor con artículos físicos. La moneda es EUR y se permite un almacén lógico por vendedor. El cliente se autentica para comprar. La administración incorpora vendedores y les asigna cuentas. Las comisiones, liquidaciones, devoluciones, fiscalidad, transportista integrado y cobros reales quedan para hitos posteriores. Antes de operar comercialmente, definir mercado, fiscalidad y obligaciones del marketplace.

## Tecnologías y decisiones de arranque

| Área | Elección | Momento |
| --- | --- | --- |
| Backend | Java 25, Spring Boot 4.1.x estable, Maven Wrapper, Spring MVC y Actuator | H0 |
| Pruebas Java | JUnit Jupiter, AssertJ, pruebas HTTP con puerto aleatorio, Failsafe para integración, JaCoCo para informes | H0; ampliar según cada hito |
| Módulos | Spring Modulith para detectar ciclos y accesos a implementación interna, ArchUnit para reglas de capas | H2, con el primer módulo real |
| Frontend | Angular 22, Node 24 compatible con el minor elegido, TypeScript estricto, componentes standalone, Vitest | H1 |
| Datos | PostgreSQL 18, Flyway como propietario del esquema, Spring Data JPA en adaptadores, Testcontainers contra PostgreSQL | H2 |
| Contratos y flujos | OpenAPI 3.1, RFC 9457 para errores, Playwright para recorridos de usuario | H1 en adelante |
| Identidad | Spring Security y OIDC con proveedor externo; sesión de servidor y cookie segura bajo el mismo origen | H3 |
| Pago | Puerto de pago y simulador local determinista con escenarios aprobado, rechazado y pendiente | H5 |
| Entrega | GitHub Actions, imágenes OCI, Docker Compose y registros estructurados | CI y backend en Compose desde H0; entorno de demostración en H7 |

Spring Boot es la elección de este plan: Actuator resuelve el arranque operativo y Spring Modulith permite verificar los límites de negocio durante el build. El sistema comienza como monolito modular, con un despliegue de backend y transacciones PostgreSQL locales. Angular se sirve bajo el mismo origen en el entorno integrado; en desarrollo puede usar un proxy. El build fija versiones exactas en Maven Wrapper, POM, archivo de versión de Node y lockfile, según el hito. Comprueba las versiones publicadas y la compatibilidad antes de fijar cada versión concreta. [Requisitos de Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) · [Actuator](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html) · [Spring Modulith](https://spring.io/projects/spring-modulith/) · [Compatibilidad de Angular](https://angular.dev/reference/versions) · [Soporte de PostgreSQL](https://www.postgresql.org/support/versioning/)

## Estructura objetivo

```text
agora/
  README.md
  docs/
    plan-agora.md
    prompt-implementacion.md
    adr/
    api/openapi.yaml
  backend/
    mvnw
    mvnw.cmd
    .mvn/wrapper/
    pom.xml
    src/main/java/com/agora/shop/
      AgoraApplication.java
      catalog/
        CatalogQueries.java
        internal/
          domain/
          application/
          adapter/in/web/
          adapter/out/persistence/
          configuration/
      sellers/
      inventory/
      cart/
      orders/
      payments/
      fulfillment/
      identity/
    src/main/resources/db/migration/
    src/test/java/com/agora/shop/
  frontend/
  .github/workflows/
```

El árbol muestra el objetivo. Crear cada directorio al incorporar su primera responsabilidad. En H0 basta el bootstrap y su prueba HTTP. Las decisiones de arquitectura llevan un ADR breve con contexto, decisión y consecuencias.

- Cada módulo es un paquete hermano y publica únicamente sus contratos de aplicación y los tipos necesarios para usarlos. Los adaptadores HTTP invocan casos de uso. Los casos de uso dependen del dominio y declaran los puertos salientes que necesitan. Los adaptadores implementan esos puertos. Spring ensambla las dependencias en la configuración del módulo.
- El dominio contiene invariantes y tipos de negocio en Java puro. JPA, Jackson, Spring, controladores y DTO HTTP quedan fuera del dominio. Mantener transacciones en la frontera del caso de uso con configuración o decoradores de infraestructura. Usar interfaces donde separen responsabilidades reales, políticas sustituibles o efectos externos.
- Cada módulo escribe sus propias tablas. Las dependencias previstas son carrito → catálogo, pedidos → carrito, catálogo, inventario y pagos. Catálogo consume el contrato de vendedores. Entrega consume eventos de pedidos y aplica acciones mediante su propio contrato. Pagos depende de su puerto de simulación. Pagos registra el resultado del simulador y publica eventos persistentes que consume pedidos; evitar una dependencia Java inversa declarando el evento en el contrato público de pagos. Identidad entrega el identificador del usuario autenticado a los adaptadores HTTP. Inventario identifica SKU mediante su ID y se coordina mediante contratos públicos; sin relaciones JPA entre módulos.
- Activar desde H2 fallos de build para ciclos, accesos a internos e importaciones de infraestructura desde dominio y aplicación. Limitar las reglas a paquetes existentes. Aplicar SOLID mediante responsabilidades pequeñas, puertos con contratos acotados y pruebas del comportamiento observable.

## Reglas de negocio y API

| Área | Comportamiento comprobable |
| --- | --- |
| Catálogo | El público ve solo SKU activos. Paginación inicial de 20, máximo 100 y orden estable con ID como desempate. Administración crea, cambia precio y activa o desactiva. Cada producto pertenece a un vendedor. El SKU público identifica la variante de forma global; el SKU comercial es único por vendedor. Los productos físicos requieren stock; los digitales requieren un recurso privado configurado. |
| Dinero | Cantidades en unidades menores enteras, moneda ISO y operaciones con control de desbordamiento. El servidor calcula precios, una tarifa por cada vendedor con líneas físicas y el total. Un pedido exclusivamente digital tiene envío de cero y prescinde de dirección postal. La orden conserva un snapshot de líneas, vendedor, tipo de producto, importes y dirección cuando aplique. Las variaciones de precio posteriores dejan intactos los pedidos existentes. |
| Carrito | Un carrito activo por cuenta, con líneas de varios vendedores y ambos tipos de producto. Cantidades de 1 a 99 por línea, con eliminación explícita. El carrito indica disponibilidad orientativa. La cotización devuelve revisión del carrito, importes y un identificador asociado a la cuenta. Checkout recibe ese identificador y la dirección si hay artículos físicos. Revalida revisión, precios, vendedores y disponibilidad. Si hay cambios devuelve 409 y exige una nueva cotización. |
| Inventario | Solo los SKU físicos generan reservas. Una reserva por pedido y SKU; cantidad disponible = existencia − reservada. Reservar todas las líneas en una transacción. Bloquear filas en orden estable de SKU o actualizar con condición atómica equivalente. Confirmar consume el stock; cancelar o expirar libera la reserva exactamente una vez. |
| Checkout | `Idempotency-Key` obligatoria, única por cuenta y operación, con hash del cuerpo normalizado. Repetición equivalente devuelve el mismo pedido; reutilización con otro cuerpo devuelve 409. Las solicitudes concurrentes equivalentes convergen en el mismo pedido. Una clave nueva para la misma revisión consumida de carrito no crea un segundo pedido. |
| Pedido | Checkout crea un pedido global del comprador y un subpedido por vendedor. El pedido conserva el importe total y cada vendedor ve solo las líneas y datos de envío necesarios de su subpedido. La transacción local crea todos los subpedidos y reserva todas las líneas físicas juntas. |
| Estados | Pago del pedido: `PENDING_PAYMENT → PAID` o `PENDING_PAYMENT → CANCELLED`. Un rechazo permite reintentar mientras el pedido siga vigente. Entrega física por vendedor: `PENDING → SHIPPED` tras el pago. Entrega digital por línea: `PENDING → AVAILABLE` tras el pago. La UI calcula el progreso con ambos tipos de entrega. Usar reloj inyectable y plazos UTC. |
| Pago y expiración | El simulador registra intentos correlacionados con el pedido, con importe y moneda verificados. Repetir un intento equivalente devuelve el mismo resultado y un evento duplicado no vuelve a consumir stock. Confirmación y expiración se serializan por pedido. Si llega una confirmación simulada tras cancelar, conservar la cancelación y registrar el conflicto sin reactivar reservas ni conceder descargas. El simulador se habilita explícitamente en perfiles local, test y demo. |
| Entrega digital | Pago confirmado concede una descarga por línea digital y comprador de forma idempotente. El archivo de demostración vive en almacenamiento privado accesible mediante un puerto. El servidor comprueba la titularidad del permiso en cada descarga. El catálogo solo expone metadatos públicos. |
| Autorización | Catálogo y estado básico públicos. Compradores acceden a sus carritos, pedidos y descargas. Roles de plataforma y vendedor explícitos; un vendedor administra sus productos, existencias y expediciones con autorización de objeto. Retornar 401 sin sesión, 403 ante rol insuficiente y 404 ante recurso de otro comprador o vendedor. Verificar autorización en servidor y CSRF en escrituras de la SPA. |
| Errores | `application/problem+json` con código estable para consumo del frontend. 400 para entrada inválida, 404 para recurso ausente y 409 para conflicto de negocio. El cuerpo omite detalles internos y secretos. |

### Contratos iniciales por hito

| Hito | Rutas |
| --- | --- |
| H0 | `GET /actuator/health` |
| H2 | `GET /api/v1/products`, `GET /api/v1/products/{id}` |
| H3 | `GET /api/v1/me`, `GET /api/v1/cart`, `PUT /api/v1/cart/items/{sku}`, `DELETE /api/v1/cart/items/{sku}` |
| H4 | `GET /api/v1/cart/quote`, `POST /api/v1/checkout`, `GET /api/v1/orders`, `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel` |
| H5 | `POST /api/v1/orders/{id}/payment-attempts`, `GET /api/v1/downloads/{entitlementId}` |
| H6 | `POST /api/v1/admin/sellers`, `PATCH /api/v1/admin/sellers/{id}`, `PUT /api/v1/admin/sellers/{id}/members/{userId}`, `GET /api/v1/sellers/{id}/orders`, `POST /api/v1/sellers/{id}/products`, `PATCH /api/v1/sellers/{id}/products/{productId}`, `POST /api/v1/sellers/{id}/inventory/{sku}/adjustments`, `POST /api/v1/sellers/{id}/orders/{suborderId}/ship` |

En cada cambio, registrar autenticación, parámetros, ejemplos de entrada y salida y errores en OpenAPI. Las nuevas versiones compatibles mantienen el significado de los campos existentes.

## Hitos y criterios de cierre

### H0 · Scaffolding del backend

1. Crear el proyecto Maven en `backend/` con Spring Initializr o un POM equivalente. Incluir starters MVC, Actuator y test, Java 25 y empaquetado ejecutable con Spring Boot. Fijar Maven Wrapper y el parche estable de Boot verificado.
2. Añadir un test de integración JUnit con `@SpringBootTest(webEnvironment = RANDOM_PORT)`. Primero observar que falla contra una aplicación con health deshabilitado o todavía sin Actuator. Después activar Actuator y comprobar en HTTP que el estado es 200, el tipo de contenido es JSON y el cuerpo contiene exactamente `{"status":"UP"}` como objeto. Comprobar que `/actuator/env` devuelve 404. Configurar exposición de `health` y ocultar sus detalles. Verificar que Failsafe ejecuta la prueba en `verify`.
3. Configurar CI con JDK 25, permisos mínimos y `./mvnw -B clean verify` desde `backend/`. Guardar informes de pruebas. Actualizar README con prerrequisitos, prueba, ejecución y salida esperada. Ignorar artefactos de compilación e IDE. Si se incorpora JaCoCo en H0, generar informe sin fijar porcentajes para el bootstrap.
4. Compilar y arrancar el JAR. Los siguientes comandos se ejecutan en terminales distintas desde la raíz:

```bash
cd backend
./mvnw -B clean verify
java -jar target/agora-backend-0.0.1-SNAPSHOT.jar
```

```bash
curl --fail --silent --show-error http://localhost:8080/actuator/health
```

**Entregable:** POM, wrapper ejecutable, bootstrap, configuración mínima, test HTTP, CI y README. **Aceptación:** comando de build con código 0, informe que muestre la prueba ejecutada y petición HTTP con código 200 y estado `UP`. Desde un clon limpio requiere JDK y acceso al repositorio Maven para resolver dependencias. H0 incluye el [arranque opcional del backend con Docker Compose](compose.md). Angular y base de datos se incorporan en sus propios hitos.

### H1 · Shell Angular e integración local

- Crear el workspace Angular, configurar proxy de desarrollo y añadir una pantalla que consulte el estado del backend. Representar carga, disponible y error con opción de reintentar; probar los tres estados con cliente HTTP simulado.
- Entregar scripts reproducibles `npm ci`, `npm run test:ci` y `npm run build`. CI ejecuta los tres y conserva informes. Documentar arranque desde dos terminales. El navegador llega a Actuator a través del proxy sin CORS permisivo. Abrir la pantalla con el backend arrancado y repetir con el backend detenido para verificar ambos resultados. Usar el [contrato de salud OpenAPI](api/openapi.yaml) y seguir el [diseño de Angular para H1](frontend-angular.md).

### H2 · Catálogo persistente

- Añadir PostgreSQL y migración Flyway, DTO públicos, lectura paginada y fixtures locales reproducibles con tienda propia, vendedor externo, SKU físico y SKU digital. Angular muestra lista, detalle, vacío y error. La administración persistente llega en H6.
- Integración PostgreSQL comprueba arranque con base vacía, reejecución de migraciones, exclusión de SKU inactivos y de vendedores inactivos, filtro por vendedor y tipo, orden, límites de página y 404. CI ejecuta pruebas de arquitectura. Aceptación manual con productos del fixture visibles en el navegador y con persistencia tras reiniciar la aplicación.

### H3 · Identidad y carrito

- Integrar OIDC local reproducible, sesión de servidor y CSRF compatible con Angular. Habilitar inicio y cierre de sesión, carrito persistente por cuenta y cambios de cantidad. Configurar cookies seguras en entorno HTTPS y tratar el perfil local explícitamente.
- Pruebas HTTP verifican 401, protección CSRF, aislamiento entre dos cuentas, rechazo de cantidades inválidas y comportamiento del carrito tras volver a iniciar sesión. E2E autentica a un usuario de prueba, añade un producto y recupera el carrito después de recargar.

### H4 · Stock y pedidos

- Incorporar inventario, reservas, cotización con tarifa por grupo físico, snapshot, checkout idempotente, creación de subpedidos, consulta y liberación de reservas. Probar la confirmación contra un doble de prueba del puerto de pago.
- Pruebas PostgreSQL con transacciones independientes coordinadas verifican dos compras sobre la última unidad, dos reintentos concurrentes de la misma clave y expiración simultánea a confirmación. Verificar rollback de todas las líneas si falla una reserva, reintento tras un error y recuperación tras reinicio del proceso durante la expiración. Demostrar desde Angular el recorrido de carrito a pedido pendiente, un cambio de precio y una compra rechazada por stock. Verificar cotizaciones física, digital y mixta, con uno y varios vendedores. Probar que un fallo de stock de cualquier vendedor impide crear el pedido completo.

### H5 · Pago simulado y entrega digital

- Implementar el puerto de pago y su adaptador local. En demo, la pantalla permite elegir aprobado o rechazado de forma explícita. Los escenarios pendientes y las confirmaciones tardías se reproducen de forma determinista en pruebas. Persistir intentos, clave idempotente, transiciones y trabajo pendiente de entrega digital. Reanudar trabajo tras reiniciar.
- Implementar el permiso de descarga y un recurso privado de demostración. Probar que un rechazo permite reintentar, que dos confirmaciones equivalentes generan un único permiso por línea y que una cuenta ajena recibe 404. Verificar que un resultado tardío tras cancelar no concede acceso. Aceptación E2E: un comprador paga un pedido digital y descarga el archivo; un comprador con pago rechazado carece de acceso. La ejecución de H5 funciona sin credenciales de cobro.

### H6 · Operación de marketplace y expedición

- Incorporar alta y desactivación de vendedores, asignación de cuentas, edición de catálogo por vendedor, ajustes de stock con motivo y expedición por subpedido. Registrar actor, acción y momento. Los ajustes respetan reservas. Solo se expiden artículos físicos de pedidos pagados y repetir la acción conserva un único resultado.
- E2E con tres recorridos: (1) compra física de la tienda propia y expedición; (2) compra física de dos vendedores, tarifa por grupo, pago global y expediciones independientes; (3) compra mixta con un grupo físico y una descarga, con tarifa solo en el grupo físico. H5 ya cubre la compra exclusivamente digital.
- Verificar que el vendedor A no lee ni modifica pedidos, stock o catálogo de B, y que un comprador carece de permisos de vendedor. Comprobar que desactivar un vendedor impide añadir sus productos y finalizar nuevas compras de esos SKU, y conserva historial y operaciones de los pedidos existentes.

### H7 · Entorno de demostración y operación

- Entregar imágenes reproducibles, configuración de ejemplo, despliegue de demostración bajo HTTPS, procedimiento de migración y restauración, y guía de incidencias. Configurar liveness independiente de PostgreSQL y readiness asociado a la disponibilidad que la instancia necesita para aceptar tráfico. Limitar la exposición operativa de Actuator y redactar datos sensibles en registros.
- Medir catálogo y escritura de checkout con datos sintéticos y registrar CPU, memoria, escenario, tiempos p50/p95 y errores. Presupuesto inicial propuesto para acordar con el entorno de demo: 10 000 SKU, 20 usuarios concurrentes durante 5 min, catálogo p95 ≤ 300 ms, checkout local p95 ≤ 700 ms y cero 5xx inesperados. Revisar planes de consulta e índices si se supera el presupuesto.
- Aceptación: despliegue desde un entorno limpio, smoke HTTP, E2E completo, restauración de una copia a base independiente y verificación de pedidos y stock. Simular pérdida de PostgreSQL y comprobar que readiness falla y liveness responde. Documentar el tiempo observado de recuperación y los límites del escenario.

## Criterio común de entrega

- Requisitos numerados por tarjeta de trabajo, ejemplos con salida esperada, pruebas ejecutables y pasos claros para ejecutarlas. Se toma de Codility el formato de tarea y validación observable mediante pruebas. [Tareas personalizadas de Codility](https://support.codility.com/hc/en-us/articles/13590752084759-Creating-Your-Own-Content-Exclusive-technology-specific-tasks)
- Usar rojo → verde → refactor sobre el comportamiento nuevo. Pruebas unitarias para invariantes; integración para adaptadores, transacciones y migraciones; E2E para recorridos críticos. La cobertura se usa para encontrar reglas sin verificar; toda regla nueva de dinero, stock, autorización o transiciones requiere un caso positivo y otro de rechazo o conflicto pertinente.
- Cada hito conserva las comprobaciones anteriores, deja actualizados README y contratos afectados y entrega evidencias de los comandos ejecutados. Revisar el diff para detectar dependencias innecesarias, secretos, fixtures globales y cambios fuera del hito. Los datos de prueba son reproducibles y las pruebas independientes del orden y del reloj real.
- Estimación inicial en jornadas de ingeniería, con una persona y experiencia en el stack: H0 1, H1 1–2, H2 3–5, H3 3–5, H4 6–10, H5 4–7, H6 5–9, H7 3–6. Total 26–45 jornadas de trabajo. Reestimar al cerrar H2 y al elegir el proveedor de identidad. Excluye esperas de cuentas externas y requisitos de salida a producción.
