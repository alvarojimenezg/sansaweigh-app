# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**SansaWeigh** is a Spring Boot 4.x microservice for a logistics company that manages
package-weighing stations. It classifies packages by weight, enforces dynamic business
rules, persists the weighing history in MongoDB, and caches scale specifications in Redis
with a resilient fallback to an external specs API.

It is a university workshop project ("TallerHDD"). The authoritative spec is
`TallerHDD.pdf`; `Plan_TallerHDD.md` is the phased implementation plan and the best
single source for *intended* behavior — but note the code has diverged from it in places
(see "Plan vs. reality" below). Trust the code over the plan when they disagree.

## Build & run

The project uses the Maven wrapper (`mvnw` / `mvnw.cmd`). Java 17+.

```bash
./mvnw clean package          # build (runs tests)
./mvnw spring-boot:run        # run the app (port 8080)
./mvnw test                   # run all tests
./mvnw test -Dtest=ClassName#methodName   # run a single test
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

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

## Conventions

- **Spanish** is used throughout domain names, comments, and exception messages
  (`RegistroPesaje`, `validarRestriccionTiempo`, etc.). Match this when adding code.
- **Lombok** for boilerplate (`@Getter/@Setter`, `@Slf4j`, `@Data/@Builder`). It's an
  annotation-processor path in `pom.xml`; keep it out of the Spring Boot fat jar.
- Services follow an **interface + `impl`** split (`services/Foo.java`,
  `services/impl/FooImpl.java`).
- Redis uses **Jedis** deliberately (`spring.data.redis.client-type=jedis`,
  `JedisConnectionFactory` in `RedisConfig`).

## Plan vs. reality — known gaps

The plan describes features the code does **not yet implement**. Don't assume these exist:

- **No `GlobalExceptionHandler` / `@RestControllerAdvice`.** The custom exceptions
  (`IllegalWeighingStateException`, `BusinessRuleException`, `ExternalScaleUnavailableException`)
  are plain `RuntimeException`s with **no `@ResponseStatus`**, so they currently surface as
  HTTP 500, not the 400 the plan specifies. Adding proper HTTP mapping is open work.
- `RegistroPesajeServiceImpl.updateEstado` throws a bare `RuntimeException` when a record
  is not found (no dedicated not-found exception / 404).
- Tests are largely deferred (the plan targets ≥90% coverage in a later phase); only the
  default Spring Boot context test exists.

## Collaboration note

This is a multi-developer repo. Per standing guidance, propose changes to files owned by
other contributors rather than editing them directly — confirm before touching code outside
the Redis/cache layer.
