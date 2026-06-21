# Plan de Implementación — TallerHDD / Microservicio "SansaWeigh"

> Spring Boot 4.x · Paquete base `cl.usm.sansaweigh`

## Contexto

El taller pide construir un **microservicio Spring Boot 4.x** para la empresa de logística
"SansaWeigh" que gestiona estaciones de pesaje de paquetes. El proyecto hoy está vacío
(solo el enunciado `TallerHDD.pdf` y apuntes de clase sobre Redis/API externa), por lo que
es un desarrollo **desde cero**.

El microservicio debe:
- Clasificar paquetes por peso y aplicar reglas de negocio dinámicas.
- Persistir el historial de pesajes en **MongoDB**.
- Cachear especificaciones de balanza en **Redis** (TTL 120s).
- Integrarse con un **registro externo de especificaciones** (vía `ExternalScaleClient`)
  con reintentos y fallback resiliente.

**Decisiones tomadas:**
- **Alcance de este plan:** solo **código core** (Mongo + Redis + integración + lógica de
  negocio + API REST). Docsify, OpenAPI/Swagger y los docs quedan para un plan aparte.
- **API externa:** se simula con **Mockoon** (sirve el JSON de specs en un endpoint HTTP
  local; la URL se inyecta por configuración con `@Value`).
- **Paquete base:** `cl.usm.sansaweigh` (groupId `cl.usm`).
- **Tests unitarios/integración (JUnit 5 + Mockito + AssertJ, 90% cobertura):** se harán
  **al final**, en una fase aparte; no son el foco ahora.

Se respeta la estructura y convenciones vistas en clase:
`configs / controllers / dto / entities / integration / repositories / services`,
uso de `RestClient`, `@RedisHash` + `CrudRepository`, `RedisConfig` con
`JedisConnectionFactory`, y Lombok para boilerplate.

---

## Estructura de paquetes objetivo

```
cl.usm.sansaweigh
├── SansaWeighApplication.java
├── configs/        RedisConfig
├── controllers/    RegistroPesajeController
├── dto/            requests/responses + ScaleSpecificationDTO
├── entities/       RegistroPesaje (Mongo), ScaleSpecification (Redis), enums
├── exceptions/     excepciones de negocio + GlobalExceptionHandler
├── integration/    ExternalScaleClient
├── repositories/   RegistroPesajeRepository (Mongo), ScaleSpecificationRepository (Redis)
└── services/       RegistroPesajeService, ScaleSpecificationService, WeighingRulesService
```

---

## Fase 0 — Setup del proyecto y entorno

**Objetivo:** dejar el esqueleto compilando y la infraestructura levantada.

1. Generar proyecto Spring Boot 4.x (Maven), Java 17+, paquete `cl.usm.sansaweigh`.
2. Dependencias (`pom.xml`):
   - `spring-boot-starter-web`
   - `spring-boot-starter-data-mongodb`
   - `spring-boot-starter-data-redis` (con cliente **Jedis**, según apuntes del profesor)
   - `lombok`
   - `spring-boot-devtools`, `spring-boot-configuration-processor`
3. Levantar infraestructura local con Docker:
   - Redis: `docker run -d --name my-redis -p 6379:6379 redis:latest`
   - MongoDB: contenedor `mongo` en `27017`.
4. `application.properties`: conexión Mongo, conexión Redis, y
   `external.scale.api.url=...` (apuntando al endpoint de **Mockoon**).
5. Crear los paquetes vacíos de la estructura.

**Resultado:** la app arranca sin errores y conecta a Mongo y Redis.

---

## Fase 1 — Modelo de dominio (entities, enums, dto)

**Objetivo:** definir las estructuras de datos antes de la lógica.

1. **Enums** en `entities/`:
   - `CategoriaPeso { LIVIANO, MEDIANO, PESADO }`
   - `EstadoPesaje { INGRESADO, PESADO, APROBADO, RECHAZADO, DESPACHADO }`
2. **`RegistroPesaje`** (`@Document`, MongoDB) — Lombok `@Data/@Builder`:
   - `id` (autogenerado), `balanzaId` (numérico), `paqueteId`,
   - `pesoSansas`, `categoria` (CategoriaPeso), `estado` (EstadoPesaje),
   - timestamps `createdAt`, `updatedAt` (y, opcional, historial de transiciones).
3. **`ScaleSpecification`** (`@RedisHash("ScaleSpecification")`, `timeToLive = 120`):
   - `id`, `name`, `brand`, `maxCapacity`, `precision`, `lastCalibrationOffset`.
   - Sigue el patrón del ejemplo `BasicItemData` del profesor.
4. **DTOs** en `dto/`:
   - `CreateRegistroPesajeRequest`, `UpdateRegistroPesajeRequest` (transición de estado),
   - `RegistroPesajeResponse`,
   - `ScaleSpecificationDTO` (shape del JSON de la API externa).

---

## Fase 2 — Persistencia (repositories + RedisConfig)

**Objetivo:** capa de acceso a datos para Mongo y Redis.

1. `repositories/RegistroPesajeRepository extends MongoRepository<RegistroPesaje, String>`
   - Método de búsqueda **filtrando por fecha** (ej. `findByCreatedAtBetween(...)`).
2. `repositories/ScaleSpecificationRepository extends CrudRepository<ScaleSpecification, String>`
   - Misma idea que `BasicItemDataRepository` del ejemplo (Redis).
3. `configs/RedisConfig` — `JedisConnectionFactory` + `RedisTemplate<String,Object>`,
   según el snippet de los apuntes (`RedisConfig.txt`).

---

## Fase 3 — Integración con API externa + fallback (integration + service)

**Objetivo:** consumir specs de balanza de forma resiliente.

1. `integration/ExternalScaleClient` (patrón del ejemplo `ItemsAPIClient`):
   - `RestClient` con `baseUrl` desde `@Value("${external.scale.api.url}")` (Mockoon).
   - Método **`getScaleSpecifications(String scaleId)`**.
   - **Reintentos exponenciales** (máx. 3) ante errores transitorios de red.
2. `services/ScaleSpecificationService` — orquesta caché + integración:
   - Intenta leer de **Redis** primero (cache hit) → si existe, retorna.
   - Si no, llama a `ExternalScaleClient`; al obtener respuesta, **guarda en Redis con TTL 120s**.
   - **Fallback:** si la API falla tras los reintentos → entrega la versión en caché;
     si no hay caché → carga la spec **por defecto desde Redis con id `"-1"`**.
   - (Se siembra una spec por defecto con id `-1` al iniciar / vía script.)

---

## Fase 4 — Lógica de negocio (services + exceptions)

**Objetivo:** reglas del enunciado. Centralizar en `WeighingRulesService` para mantener el
`RegistroPesajeService` limpio.

1. **Conversión de unidades:** 1 Sansa = `1.337 kg`. El sistema opera en Sansas
   (utilidad de conversión Sansa↔kg).
2. **Clasificación por peso (Sansas):**
   - `LIVIANO` ≤ 10 · `MEDIANO` > 10 y ≤ 50 · `PESADO` > 50.
3. **Restricciones de procesamiento:**
   - **Horaria:** prohibido pesar/procesar paquetes `PESADO` entre **20:00 y 06:00**
     (hora del servidor).
   - **Balanza Prima:** si `balanzaId` es **primo**, no puede registrar `PESADO` en
     **días calendario impares** del mes → lanza excepción de negocio.
4. **Máquina de estados:** `INGRESADO → PESADO → APROBADO|RECHAZADO → DESPACHADO`.
   - A `APROBADO`/`RECHAZADO` solo desde `PESADO`.
   - Transición inválida → `IllegalWeighingStateException` (HTTP **400**).
5. **Excepciones** en `exceptions/`:
   - `IllegalWeighingStateException`, `BusinessRuleException` (horaria/prima),
   - `GlobalExceptionHandler` (`@RestControllerAdvice`) → mapea a códigos HTTP (400, etc.).

---

## Fase 5 — API REST (controllers)

**Objetivo:** exponer las operaciones requeridas.

`controllers/RegistroPesajeController` (patrón `BasicItemsController`, `ResponseEntity`):
- **POST** `/registros` — crear registro (aplica clasificación + restricciones).
- **PUT/PATCH** `/registros/{id}/estado` — actualizar/transicionar estado (valida máquina de estados).
- **GET** `/registros?desde=...&hasta=...` — obtener registros **filtrando por fecha**.
- (Opcional) **GET** `/balanzas/{scaleId}/specs` — exponer specs vía `ScaleSpecificationService`.

---

## Fase 6 — Pruebas (al final, fuera del foco actual)

Reservada para el cierre: JUnit 5 + Mockito + AssertJ apuntando a **≥90% de cobertura**
(servicios de reglas, máquina de estados, fallback Redis, integración con Mockoon).
Se detallará en su propio plan.

---

## Verificación (end-to-end del código core)

1. `docker` con Redis y Mongo arriba; **Mockoon** sirviendo el JSON de specs.
2. `mvn spring-boot:run` arranca sin errores.
3. Crear un registro (POST) y verificar clasificación correcta por peso en Sansas.
4. Probar transición inválida de estado → debe responder **400** con
   `IllegalWeighingStateException`.
5. Probar paquete `PESADO` en horario nocturno y en balanza prima/día impar → excepción de negocio.
6. Consultar specs con Mockoon **encendido** → respuesta + entrada cacheada en Redis (RedisInsight),
   con TTL ~120s.
7. **Apagar Mockoon** y volver a consultar → debe devolver la caché; si se limpia la caché,
   debe devolver la spec por defecto con id `-1`.
8. GET con filtro de fechas → retorna solo los registros del rango.
