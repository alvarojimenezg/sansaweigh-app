# Arquitectura

Paquete base: `cl.sansaweigh.sansaweighapp`. Estructura en capas:

```
configs/        RedisConfig, OpenApiConfig, DefaultSpecificationSeeder
controllers/    RegistroPesajeController, ScaleSpecificationController
dto/            requests / responses + ScaleSpecificationDTO
entities/       RegistroPesaje (Mongo), ScaleSpecification (Redis), enums
exceptions/     excepciones de negocio + GlobalExceptionHandler
integration/    ExternalScaleClient
repositories/   RegistroPesajeRepository (Mongo), ScaleSpecificationRepository (Redis)
services/       RegistroPesajeService, ScaleSpecificationService, WeighingRulesService
```

## Dos flujos de datos independientes

### 1. Registros de pesaje (MongoDB, durable)

```
RegistroPesajeController → RegistroPesajeService → RegistroPesajeRepository → MongoDB
```

La entidad `RegistroPesaje` (`@Document`) guarda el peso en Sansas, la categoría, el estado,
los timestamps y un `historialTransiciones`. La lógica de negocio se delega íntegramente en
`WeighingRulesService`.

### 2. Especificaciones de balanza (Redis + API externa, efímero)

```
ScaleSpecificationController → ScaleSpecificationService → Redis (cache)
                                                        ↘ ExternalScaleClient → API externa
```

`ScaleSpecification` es `@RedisHash(timeToLive = 120)` → **TTL de 120 segundos**.

Estos dos flujos son independientes: crear un registro de pesaje **no** consulta las specs.

## Resiliencia del flujo de specs

`ScaleSpecificationService` aplica una cadena de fallback:

1. **Cache hit** en Redis → retorna de inmediato.
2. **Cache miss** → llama a `ExternalScaleClient`; si responde, guarda en Redis (TTL 120 s) y retorna.
3. **API caída** → `ExternalScaleClient` reintenta hasta **3 veces** con backoff exponencial
   (500 ms → 1000 ms) y luego lanza `ExternalScaleUnavailableException`.
4. **Fallback final** → se entrega la **especificación por defecto con id `"-1"`**, sembrada al
   arranque por `DefaultSpecificationSeeder` (y recreada de forma perezosa si expiró).

## Manejo de errores

`GlobalExceptionHandler` (`@RestControllerAdvice`) mapea las excepciones a códigos HTTP
coherentes y devuelve un cuerpo JSON uniforme (`timestamp`, `status`, `error`, `message`).
Ver detalle en [API REST](api.md#codigos-de-error).
