# Configuración y ejecución

## Requisitos

- Java 17+
- Maven (se incluye el wrapper `mvnw` / `mvnw.cmd`)
- Docker (para Redis y MongoDB)
- [Mockoon](https://mockoon.com/) (para simular la API externa de specs)

## Infraestructura

```bash
# Redis (cliente Jedis)
docker run -d --name my-redis -p 6379:6379 redis:latest

# MongoDB (base de datos "sansaweigh")
docker run -d --name mongo -p 27017:27017 mongo:latest
```

Si los contenedores ya existen, basta con arrancarlos:

```bash
docker start my-redis mongo
```

**Mockoon** debe servir las specs en `http://localhost:3000`, endpoint
`GET /scales/{scaleId}`, devolviendo un JSON con la forma de `ScaleSpecificationDTO`.

## Configuración

Todo vive en `src/main/resources/application.properties`:

| Propiedad | Valor |
|-----------|-------|
| MongoDB | `localhost:27017`, base `sansaweigh` |
| Redis | `localhost:6379`, cliente `jedis` |
| API externa | `external.scale.api.url=http://localhost:3000` |

## Compilar y ejecutar

```bash
./mvnw clean package          # build (corre los tests)
./mvnw spring-boot:run        # levanta la app (puerto 8080)
./mvnw test                   # corre todos los tests
```

En Windows usa `mvnw.cmd` en vez de `./mvnw`.

- **Health check**: <http://localhost:8080/actuator/health>
- **Swagger UI**: <http://localhost:8080/swagger-ui.html>

## Ver esta documentación (Docsify)

Desde la carpeta `docs/`:

```bash
# opción 1: con docsify-cli
npm i -g docsify-cli
docsify serve docs

# opción 2: cualquier servidor estático
python -m http.server 3001 --directory docs
```

Luego abre <http://localhost:3000> (docsify-cli) o el puerto que hayas elegido.
