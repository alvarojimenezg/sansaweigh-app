# SansaWeigh

Microservicio **Spring Boot 4.x** para una empresa de logística que gestiona estaciones de
pesaje de paquetes. Clasifica paquetes por peso, aplica reglas de negocio dinámicas, persiste
el historial de pesajes en **MongoDB** y cachea las especificaciones de balanza en **Redis**
con un fallback resiliente a una API externa.

> Proyecto del taller universitario **TallerHDD**.

## Qué hace

- **Clasifica** cada paquete por su peso (en *Sansas*) en `LIVIANO`, `MEDIANO` o `PESADO`.
- Aplica **reglas de negocio**: restricción horaria y de balanza prima para paquetes pesados.
- Gestiona el ciclo de vida del pesaje con una **máquina de estados**.
- **Persiste** el historial de pesajes en MongoDB.
- **Cachea** las specs de balanza en Redis (TTL 120 s) con **fallback** a la API externa y, en
  último caso, a una especificación por defecto.

## Stack

| Componente | Tecnología |
|------------|------------|
| Framework | Spring Boot 4.1 · Java 17 |
| Persistencia durable | MongoDB |
| Caché | Redis (cliente **Jedis**) |
| API externa (mock) | Mockoon |
| Documentación API | springdoc-openapi / Swagger UI |

## Documentación interactiva (Swagger)

Con la app corriendo:

- **Swagger UI** → <http://localhost:8080/swagger-ui.html>
- **OpenAPI JSON** → <http://localhost:8080/v3/api-docs>

## Secciones

- [Arquitectura](arquitectura.md) — capas, flujos de datos y resiliencia.
- [Reglas de negocio](reglas-negocio.md) — conversión, clasificación, restricciones y estados.
- [API REST](api.md) — endpoints, ejemplos de request/response y códigos HTTP.
- [Configuración y ejecución](setup.md) — cómo levantar la infraestructura y la app.
