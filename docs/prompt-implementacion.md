# Prompt de implementación para un agente de código

Copia el bloque siguiente al agente que vaya a implementar Ágora. Facilítale acceso al repositorio y al archivo `docs/plan-agora.md`.

```text
Implementa Ágora en el repositorio que tienes abierto. El MVP incluye tienda
propia, marketplace de varios vendedores y productos físicos y digitales.
La aceptación final usa pago simulado y tarifa fija por vendedor con envío
físico. Confirma los tres recorridos de negocio según H5 y H6.
Lee docs/plan-agora.md completo antes de editar. Ese documento fija el alcance,
la arquitectura, las pruebas de aceptación y el orden H0 a H7.

Tu encargo por defecto es H0 únicamente. Si la persona indica otro hito, ejecuta
ese hito y verifica sus prerrequisitos. Si pide un intervalo, cierra cada hito
antes de pasar al siguiente. Si H0 ya existe, primero verifica sus criterios
desde el estado actual y corrige únicamente los incumplimientos de H0.

Procedimiento para cada hito

1. Revisa Git, instrucciones del repositorio, archivos existentes y herramientas.
   Preserva cambios ajenos. Anota brevemente qué requisitos aplicarás y qué
   comandos usarás para verificar cada uno.
2. Lee las reglas de negocio y contratos de ese hito. Divide el trabajo en
   cambios pequeños. Por cada comportamiento, añade una prueba que falle
   por la razón esperada, ejecuta la prueba, implementa el mínimo que la haga
   pasar, y refactoriza con las pruebas en verde. Al tocar código previo,
   conserva los contratos y pruebas que ya existían.
3. Usa el empaquetado por módulo de negocio y la separación de capas del plan.
   Crea paquetes al añadir código con responsabilidad concreta. Prueba el
   dominio con Java puro. Ejecuta pruebas HTTP contra servidor real cuando
   verifiques enrutamiento, wiring, filtros o configuración. Usa PostgreSQL
   real en pruebas de migraciones, constraints y concurrencia.
4. Fija versiones estables exactas compatibles en los archivos de proyecto.
   Conserva lockfiles y wrappers. Documenta comandos desde un clon limpio.
   Usa configuración de entorno y valores ficticios en ejemplos.
5. Ejecuta todas las comprobaciones del hito y las suites anteriores.
   Lee los informes y comprueba que las pruebas previstas se ejecutaron.
   Arranca los artefactos finales y repite la aceptación manual definida.
   Si una comprobación falla, diagnostica, corrige y vuelve a ejecutarla.
   Si una dependencia externa impide verificar, informa del bloqueo concreto,
   el comando, el resultado observado y el paso necesario para desbloquear.
6. Inspecciona el diff y git status. Entrega el código y la documentación
   en el repositorio. Reporta identificador de hito, cambios, pruebas,
   resultados observados, criterios pendientes y decisiones tomadas.
   Indica la ubicación de los informes. Declara el hito completo solo
   con evidencia de todos sus criterios. No hagas push ni despliegues
   externos sin autorización expresa.

Tarjeta H0: arrancar y comprobar Spring Boot

H0.1 Genera backend/ para Java 25, Spring Boot 4.1.x estable y Maven Wrapper.
     Usa groupId com.agora, artifactId agora-backend, base com.agora.shop,
     versión inicial 0.0.1-SNAPSHOT. El JAR esperado es
     backend/target/agora-backend-0.0.1-SNAPSHOT.jar.
H0.2 Con la aplicación mínima creada, añade HealthEndpointIT como prueba
     de integración a puerto aleatorio. Inclúyela en Maven Failsafe con
     integration-test y verify. Verifica el fallo de la prueba de health
     antes de habilitar el endpoint. Añade Actuator y configura la API
     operativa para exponer health sin detalles. La prueba requiere
     GET /actuator/health = HTTP 200, JSON con el único campo status=UP,
     y GET /actuator/env = HTTP 404. Evita comprobar orden de campos
     o formato textual. Usa timeouts en cliente HTTP.
H0.3 Añade CI en GitHub Actions con Java 25 y Maven Wrapper. Configúrala
     para push y pull_request, con working-directory backend y permisos
     contents: read. Ejecuta ./mvnw -B clean verify y publica informes
     de prueba incluso en caso de fallo.
H0.4 Actualiza README con requisitos, pasos de verificación y ejecución.
     Añade .gitignore si hace falta. Ejecuta cd backend && ./mvnw -B clean verify.
     Comprueba el informe de HealthEndpointIT y arranca el JAR generado
     con java -jar. Comprueba el puerto y consulta el endpoint con curl
     o Postman CLI. Comprueba HTTP 200 y status UP. Detén el proceso
     que hayas arrancado. Si 8080 está ocupado, usa un puerto libre y
     registra cuál empleaste.
H0.5 Comprueba que el arranque y las pruebas de H0 funcionan con JDK 25
     y el Wrapper, sin Node, Docker, base de datos ni credenciales.
     H0 termina con el scaffold Java, configuración, prueba HTTP, CI
     y documentación. Angular comienza en H1.
H0.6 Verifica también el arranque desde la raíz con Docker Compose, según
     docs/compose.md. Ejecuta docker compose up --build --wait, comprueba
     el endpoint en el puerto publicado y termina con docker compose down.

Entrega final del hito

- Cambios y rutas de archivos.
- Evidencia por requisito: comando, código de salida y resultado relevante.
- Informes de pruebas y verificación del artefacto en ejecución.
- Decisiones tomadas y criterios pendientes, si los hubiera.
```
