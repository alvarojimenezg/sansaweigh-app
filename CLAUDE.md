# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Qué es este proyecto

Microservicio Spring Boot para "SansaWeigh", una empresa de logística que gestiona estaciones
de pesaje de paquetes. Es un taller académico (UTFSM). El enunciado completo y la rúbrica de
evaluación están en `TallerHDD.pdf`; el plan de implementación por fases en `Plan_TallerHDD.md`.

- **Spring Boot 4.1.0**, **Java 17**, Maven.
- **Paquete base real: `cl.sansaweigh.sansaweighapp`** (groupId `cl.sansaweigh`).
  Nota: `Plan_TallerHDD.md` menciona `cl.usm.sansaweigh` como objetivo, pero el código vive en
  `cl.sansaweigh.sansaweighapp` — usa este último.

## Comandos

El proyecto usa el Maven Wrapper. En Windows (shell principal de este repo) es `mvnw.cmd`;
en bash/Git Bash usa `./mvnw`.

```bash
mvnw.cmd clean compile          # compilar
mvnw.cmd spring-boot:run        # levantar la app (puerto 8080 por defecto)
mvnw.cmd test                   # correr toda la suite de tests
mvnw.cmd test -Dtest=NombreClaseTest             # un solo test (clase)
mvnw.cmd test -Dtest=NombreClaseTest#nombreMetodo # un solo método de test
mvnw.cmd clean package          # build del jar
```

## Infraestructura local requerida

La app espera estos servicios levantados (config en `src/main/resources/application.properties`):

- **MongoDB** en `localhost:27017`, base de datos `sansaweigh`.
- **Redis** en `localhost:6379` (cliente **Jedis**, no Lettuce — fijado por `spring.data.redis.client-type=jedis`).
- **API externa de specs de balanza** en `http://localhost:3000`, simulada con **Mockoon**
  (URL inyectada vía `external.scale.api.url`).

Actuator health (`/actuator/health`) está expuesto con detalles para verificar conexión a
Mongo y Redis.

## Arquitectura y reglas de negocio

Arquitectura en capas estándar Spring: `controllers → services → repositories`, con
`dto / entities / exceptions / integration / configs`. Las interfaces de servicio viven en
`services/` y sus implementaciones en `services/impl/`.

**Concepto central — toda la lógica de negocio se centraliza en `WeighingRulesService`**
(`services/impl/WeighingRulesServiceImpl`), para mantener limpio el `RegistroPesajeService`.
Antes de tocar reglas, leer esta clase. Reglas implementadas:

- **Unidad propietaria:** el sistema opera en **Sansas**, no kg. `1 Sansa = 1.337 kg`
  (constante `CONVERSION_RATIO`). El peso entra en kg y se convierte antes de clasificar.
- **Clasificación por peso (en Sansas):** `LIVIANO ≤ 10` · `MEDIANO > 10 y ≤ 50` · `PESADO > 50`.
- **Restricción horaria:** paquetes `PESADO` no se procesan entre 20:00 y 06:00 (hora del servidor).
- **Regla de balanza prima:** si `balanzaId` es primo, no puede registrar `PESADO` en días
  calendario impares → `BusinessRuleException`.
- **Máquina de estados:** `INGRESADO → PESADO → APROBADO|RECHAZADO → DESPACHADO`. A
  `APROBADO`/`RECHAZADO` solo se llega desde `PESADO`. Transición inválida →
  `IllegalWeighingStateException` (debe mapear a HTTP 400).

**Persistencia:**
- `RegistroPesaje` es un `@Document` de **MongoDB** (historial de pesajes), con `createdAt`,
  `updatedAt` e historial de transiciones. Se consulta filtrando por fecha
  (`findByCreatedAtBetween`).
- `ScaleSpecification` es un `@RedisHash` con **TTL de 120s** (caché de specs de balanza).

**Integración + fallback (en construcción):** `ExternalScaleClient` (con `RestClient` y
reintentos exponenciales, máx. 3) consulta specs a la API externa. Estrategia de resiliencia:
cache hit en Redis → si no, llamar API y cachear con TTL 120s → si la API falla, devolver la
caché → si no hay caché, cargar la spec por defecto desde Redis con `id "-1"`.

## Convenciones del proyecto

- **Lombok:** usar **`@Getter` / `@Setter` / `@ToString`** (más `@NoArgsConstructor` /
  `@AllArgsConstructor` donde aplique). El proyecto **NO** usa `@Data` ni `@Builder` — fueron
  retirados deliberadamente porque nada del código depende de `equals/hashCode` ni del patrón
  builder. Mantener este estilo al crear entidades/DTOs nuevos.
- **Validación Bean Validation** (`jakarta.validation`): usar la anotación correcta según el
  tipo — `@NotBlank` solo para `String`, `@Positive`/`@NotNull` para numéricos.
- **Workflow Git:** se trabaja en la rama `dev` (y ramas `*-dev` por integrante que mergean a
  `dev` vía PR). No commitear directo a `main`.

## Estado del desarrollo

El trabajo sigue las fases de `Plan_TallerHDD.md`. Hecho: modelo de dominio (entities/enums/DTOs)
y lógica de negocio (`WeighingRulesService`, máquina de estados) + persistencia Mongo
(`RegistroPesajeService`). Pendiente: `RedisConfig`, `ScaleSpecificationRepository`,
`ExternalScaleClient` + `ScaleSpecificationService`, `GlobalExceptionHandler`, los controllers
REST, y la suite de tests (la rúbrica exige **≥90% de cobertura de líneas** con JUnit 5 +
Mockito + AssertJ). También pendientes como entregables: documentación **Docsify** en `docs/`
(reemplazando el README) y **Swagger UI / OpenAPI** (`docs/openapi.yaml`).
