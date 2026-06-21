# Objetivos del Proyecto SansaWeigh

Este documento resume los objetivos principales extraídos de `TallerHDD.pdf` y `Plan_TallerHDD.md` para el desarrollo del microservicio "SansaWeigh".

## 1. Arquitectura y Tecnologías
- **Framework:** Spring Boot 4.x (Java 17+).
- **Persistencia de Datos:** MongoDB (Spring Data MongoDB) para almacenar el historial de pesajes.
- **Caché:** Redis (Spring Data Redis con Jedis) para guardar configuraciones de balanzas con un TTL de 120 segundos.
- **Librerías Adicionales:** Lombok para reducir boilerplate.

## 2. Dominio y Reglas de Negocio
- **Unidad de Medida:** Sistema basado en "Sansas" (1 Sansa = 1.337 kg).
- **Clasificación de Paquetes:**
  - *Liviano:* $\le$ 10 Sansas.
  - *Mediano:* > 10 y $\le$ 50 Sansas.
  - *Pesado:* > 50 Sansas.
- **Restricciones de Procesamiento:**
  - *Horaria:* Prohibido procesar paquetes "Pesado" entre las 20:00 y las 06:00 hrs.
  - *Balanza Prima:* Balanzas con ID número primo no pueden registrar paquetes "Pesado" en días impares del mes.
- **Máquina de Estados:** `INGRESADO` $\rightarrow$ `PESADO` $\rightarrow$ `APROBADO` o `RECHAZADO` $\rightarrow$ `DESPACHADO`. Transiciones inválidas arrojan `IllegalWeighingStateException` (HTTP 400).

## 3. Integración Externa
- Consumir una API externa (`ExternalScaleClient`) para obtener especificaciones de las balanzas.
- Implementar **reintentos exponenciales** (máx. 3) en caso de fallos de red.
- **Fallback:** Si la API falla, devolver la versión en caché de Redis. Si no hay caché, retornar una especificación por defecto almacenada en Redis con el ID `"-1"`.

## 4. Pruebas y Documentación (Entregables Finales)
- **Pruebas:** JUnit 5, Mockito y AssertJ con cobertura mínima del **90%** (Line Coverage).
- **Documentación:** Reemplazar `README.md` por una guía interactiva con **Docsify**.
- **API:** Especificación en formato OpenAPI 3.0/3.1 e interfaz **Swagger UI** expuesta mediante Docsify.
