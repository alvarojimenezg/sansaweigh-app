# SansaWeigh

Microservicio **Spring Boot 4.x** para una empresa de logística que gestiona estaciones de
pesaje de paquetes: clasifica por peso, aplica reglas de negocio, persiste el historial en
**MongoDB** y cachea las especificaciones de balanza en **Redis** con fallback resiliente a
una API externa.

> Proyecto del taller universitario **TallerHDD**.

## Inicio rápido

```bash
# Infraestructura
docker run -d --name my-redis -p 6379:6379 redis:latest
docker run -d --name mongo    -p 27017:27017 mongo:latest
# (y Mockoon sirviendo specs en http://localhost:3000)

# App
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

## Documentación

- **Swagger UI** (API interactiva): <http://localhost:8080/swagger-ui.html>
- **OpenAPI JSON**: <http://localhost:8080/v3/api-docs>
- **Documentación completa (Docsify)**: carpeta [`docs/`](docs/) → `docsify serve docs`
- **Health check**: <http://localhost:8080/actuator/health>

Para el detalle de arquitectura, reglas de negocio, endpoints y configuración, revisa el
sitio Docsify en `docs/`.
