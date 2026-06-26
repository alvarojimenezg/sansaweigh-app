# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**SansaWeigh** is a Spring Boot 4.x microservice for a logistics company that manages
package-weighing stations. It classifies packages by weight, enforces dynamic business
rules, persists the weighing history in MongoDB, and caches scale specifications in Redis
with a resilient fallback to an external specs API.

It is a university workshop project ("TallerHDD"). The authoritative spec is
`TallerHDD.pdf`; `Plan_TallerHDD.md` is the phased implementation plan. The plan still uses
the old base package `cl.usm.sansaweigh` and describes some features differently from how
they were eventually built — **trust the code over the plan when they disagree.** All phases
(domain, persistence, Redis/external integration, business rules, REST API, exception
handling, docs, and tests) are now implemented.

## Build & run

The project uses the Maven wrapper (`mvnw` / `mvnw.cmd`). Java 17+.

```bash
./mvnw clean package          # build (runs tests)
./mvnw spring-boot:run        # run the app (port 8080)
./mvnw test                   # run all tests
./mvnw test -Dtest=ClassName#methodName   # run a single test
./mvnw verify                 # tests + JaCoCo coverage gate (fails under 90% line coverage)
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

API docs (app running): **Swagger UI** at `/swagger-ui.html`, **OpenAPI JSON** at `/v3/api-docs`.
Long-form docs live in `docs/` as a **Docsify** site (`docsify serve docs`).

### Required infrastructure

The app will not start cleanly without Redis and MongoDB reachable, and the external-specs
flow expects a mock API:

```bash
docker run -d --name my-redis -p 6379:6379 redis:latest
docker run -d --name mongo    -p 27017:27017 mongo:latest
```

- **MongoDB** — `localhost:27017`, database `sansaweigh`
- **Redis** — `localhost:6379`, **Jedis** client (not Lettuce)
- **External scale API** — simulated with **Mockoon** serving specs at `http://localhost:3000`
  (endpoint `GET /scales/{scaleId}`). URL injected via `external.scale.api.url`.

All connection settings live in `src/main/resources/application.properties`.
Actuator health (including Redis/Mongo) is at `/actuator/health`.

## Architecture

Base package: `cl.sansaweigh.sansaweighapp` (note: **not** `cl.usm.sansaweigh` from the plan).
Layered structure under that package: `configs / controllers / dto / entities /
exceptions / integration / repositories / services` (with `services/impl`).

### Two distinct data flows

1. **Weighing records (MongoDB, durable).** `RegistroPesajeController` → `RegistroPesajeService`
   → `RegistroPesajeRepository` (`MongoRepository`). The `RegistroPesaje` entity (`@Document`)
   holds the weight in Sansas, category, state, timestamps, and a `historialTransiciones` log.

2. **Scale specs (Redis cache + external API, ephemeral).** `ScaleSpecificationService`
   reads Redis first; on a miss it calls `ExternalScaleClient` and caches the result.
   The `ScaleSpecification` entity is `@RedisHash(timeToLive = 120)` — **120-second TTL**.

These two flows are independent: weighing-record creation does **not** currently consult
scale specs.

### Business rules — all centralized in `WeighingRulesServiceImpl`

This is the heart of the domain. `RegistroPesajeService` stays thin and delegates here.

- **Unit conversion:** 1 Sansa = `1.337 kg`. Requests come in **kg**; the system stores and
  classifies in **Sansas** (`deKgToSansa`).
- **Classification (in Sansas):** `LIVIANO` ≤ 10 · `MEDIANO` > 10 and ≤ 50 · `PESADO` > 50.
- **Time restriction:** `PESADO` packages cannot be processed between **20:00 and 06:00**
  (server time).
- **Prime-scale restriction:** if `balanzaId` is **prime**, it cannot register `PESADO`
  on **odd calendar days** of the month.
- **State machine:** `INGRESADO → PESADO → APROBADO|RECHAZADO → DESPACHADO`. `DESPACHADO`
  is terminal. Invalid transitions throw `IllegalWeighingStateException`.

When changing weight thresholds, the conversion ratio, or the state machine, edit
`WeighingRulesServiceImpl` — not the controllers or `RegistroPesajeServiceImpl`.

### Resilience in the spec flow

`ExternalScaleClient` retries the external API up to **3 times** with exponential backoff
(500ms → 1000ms), then throws `ExternalScaleUnavailableException`.
`ScaleSpecificationServiceImpl` handles the fallback chain: Redis cache hit → external API
(cache the result) → on failure, a **default spec with id `"-1"`** (lazily recreated via
`getDefaultSpecification()` if it expired or was never seeded). `DefaultSpecificationSeeder`
seeds that default at startup.

### HTTP error mapping — `GlobalExceptionHandler`

`exceptions/GlobalExceptionHandler` (`@RestControllerAdvice`) is the single place that maps
exceptions to HTTP status with a uniform JSON body (`timestamp`, `status`, `error`, `message`):
`IllegalWeighingStateException` / `BusinessRuleException` / `IllegalArgumentException` /
`@Valid` failures → **400**; `RegistroNoEncontradoException` → **404**;
`ExternalScaleUnavailableException` → **503**. The custom exceptions are plain
`RuntimeException`s with **no `@ResponseStatus`** — their HTTP code comes only from this
handler, so add new mappings here rather than annotating the exceptions.

## Conventions

- **Spanish** is used throughout domain names, comments, and exception messages
  (`RegistroPesaje`, `validarRestriccionTiempo`, etc.). Match this when adding code.
- **Lombok** for boilerplate. The standing convention is **`@Getter/@Setter/@ToString` +
  `@NoArgsConstructor/@AllArgsConstructor`** — **not** `@Data`/`@Builder` (nothing uses
  `.builder()` or generated `equals`/`hashCode`). It's an annotation-processor path in
  `pom.xml`; keep it out of the Spring Boot fat jar.
- Services follow an **interface + `impl`** split (`services/Foo.java`,
  `services/impl/FooImpl.java`).
- Redis uses **Jedis** deliberately (`spring.data.redis.client-type=jedis`,
  `JedisConnectionFactory` in `RedisConfig`).

## Testing

Tests live under `src/test/java/...` mirroring the main package layout. Stack: **JUnit
Jupiter 6 + Mockito 5 + AssertJ** (all transitive via the `*-test` starters; no explicit
deps needed). Services are tested with `@Mock`/`@InjectMocks`; controllers with
`@WebMvcTest` + MockMvc + `@MockitoBean` (note: `@MockBean` is gone in Spring Boot 4, use
`@MockitoBean`). **JaCoCo** enforces **≥90% line coverage** on `mvnw verify`; the report is
at `target/site/jacoco/index.html`. The bootstrap class and `integration/ExternalScaleClient`
are excluded from the coverage metric in `pom.xml` (the client's `RestClient` is built
internally and is covered only indirectly via the service fallback).

## Spring Boot 4 gotchas

- **Jackson 3, not 2.** Spring Boot 4 ships Jackson 3 (`tools.jackson`). Jackson-2 helpers
  like `GenericJackson2JsonRedisSerializer` throw `NoClassDefFoundError` at runtime — avoid
  them (`RedisConfig` uses only `StringRedisSerializer` for keys).
- **Test slices moved packages.** `@WebMvcTest` is now
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, not the old
  `...boot.test.autoconfigure.web.servlet` path.
- **API docs use springdoc 3.0.x** (the only line compatible with Spring Boot 4). **Do not
  re-add `spring-boot-starter-data-rest`** — it auto-exposes the repositories and makes
  springdoc throw a `NullPointerException` when generating `/v3/api-docs`.
- Editing `pom.xml` dependencies is **not** picked up by `spring-boot-devtools` hot restart;
  stop and relaunch the app fully.

## Collaboration note

This is a multi-developer repo. Per standing guidance, propose changes to files owned by
other contributors rather than editing them directly — confirm before touching code outside
the Redis/cache layer.
