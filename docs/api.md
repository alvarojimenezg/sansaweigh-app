# API REST

URL base: `http://localhost:8080`

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/registros` | Crear un registro de pesaje |
| `PUT` / `PATCH` | `/registros/{id}/estado` | Transicionar el estado de un registro |
| `GET` | `/registros?desde=&hasta=` | Listar registros por rango de fechas |
| `GET` | `/balanzas/{scaleId}/specs` | Obtener specs de balanza (caché + fallback) |

> Documentación interactiva: [Swagger UI](http://localhost:8080/swagger-ui.html).

## Crear registro

`POST /registros`

```json
{
  "balanzaId": 101,
  "paqueteId": "PKG-001",
  "pesoKg": 10
}
```

**201 Created** — la categoría y el estado los deriva el servidor:

```json
{
  "id": "6629f1a3b2c4d5e6f7a8b9c0",
  "balanzaId": 101,
  "paqueteId": "PKG-001",
  "pesoSansas": 7.48,
  "categoria": "LIVIANO",
  "estado": "INGRESADO",
  "createdAt": "2026-06-24T13:39:52",
  "updatedAt": "2026-06-24T13:39:52",
  "historialTransiciones": ["Creado en estado INGRESADO a las 2026-06-24T13:39:52"]
}
```

## Transicionar estado

`PUT /registros/{id}/estado`

```json
{ "nuevoEstado": "PESADO" }
```

**200 OK** con el registro actualizado. Una transición no permitida por la
[máquina de estados](reglas-negocio.md#maquina-de-estados) responde **400**.

## Listar por fecha

`GET /registros?desde=2026-06-01T00:00:00&hasta=2026-06-30T23:59:59`

**200 OK** con la lista de registros creados dentro del rango.

## Specs de balanza

`GET /balanzas/{scaleId}/specs`

**200 OK**. Primero busca en Redis; si no está, consulta la API externa y cachea el resultado
(TTL 120 s). Si la API no responde tras los reintentos, devuelve la spec por defecto `id: "-1"`.

```json
{
  "id": "101",
  "name": "Balanza Central Sur",
  "brand": "SansaScale-Pro",
  "maxCapacity": 150.0,
  "precision": 0.01,
  "lastCalibrationOffset": -0.0
}
```

## Códigos de error

Todas las respuestas de error comparten el mismo cuerpo:

```json
{
  "timestamp": "2026-06-24T13:39:52",
  "status": 400,
  "error": "Bad Request",
  "message": "Transición de estado inválida. No se puede pasar de PESADO a DESPACHADO"
}
```

| Situación | Excepción | HTTP |
|-----------|-----------|------|
| Transición de estado inválida | `IllegalWeighingStateException` | **400** |
| Regla de negocio (horaria / prima) | `BusinessRuleException` | **400** |
| Argumento inválido (p. ej. peso negativo) | `IllegalArgumentException` | **400** |
| Validación de campos del request (`@Valid`) | `MethodArgumentNotValidException` | **400** |
| Registro no encontrado | `RegistroNoEncontradoException` | **404** |
| API externa de specs no disponible | `ExternalScaleUnavailableException` | **503** |
