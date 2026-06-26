# Reglas de negocio

Todas las reglas están centralizadas en `WeighingRulesServiceImpl`.

## Conversión de unidades

El sistema opera internamente en **Sansas**. Las peticiones llegan en **kg**.

$$1\ \text{Sansa} = 1.337\ \text{kg} \qquad \text{Sansas} = \frac{kg}{1.337}$$

Un peso negativo lanza `IllegalArgumentException` (→ HTTP 400).

## Clasificación por peso (en Sansas)

| Categoría | Rango (Sansas) | Equivalente aprox. (kg) |
|-----------|----------------|--------------------------|
| `LIVIANO` | ≤ 10 | ≤ 13.37 kg |
| `MEDIANO` | > 10 y ≤ 50 | 13.37 – 66.85 kg |
| `PESADO` | > 50 | > 66.85 kg |

## Restricciones para paquetes `PESADO`

### Restricción horaria

No se permite procesar paquetes `PESADO` entre las **20:00 y las 06:00** (hora del servidor).
Violarla lanza `BusinessRuleException` (→ HTTP 400).

### Restricción de balanza prima

Si el `balanzaId` es un **número primo**, no puede registrar paquetes `PESADO` en **días
impares** del mes. Violarla lanza `BusinessRuleException` (→ HTTP 400).

## Máquina de estados

```
INGRESADO → PESADO → APROBADO ⟍
                  ↘ RECHAZADO  → DESPACHADO
```

| Estado actual | Transiciones válidas |
|---------------|----------------------|
| *(nuevo)* | `INGRESADO` |
| `INGRESADO` | `PESADO` |
| `PESADO` | `APROBADO`, `RECHAZADO` |
| `APROBADO` | `DESPACHADO` |
| `RECHAZADO` | `DESPACHADO` |
| `DESPACHADO` | *(terminal — ninguna)* |

Cualquier transición fuera de esta tabla lanza `IllegalWeighingStateException` (→ HTTP 400).
Cada transición válida queda registrada en el `historialTransiciones` del registro.

> Nota: `PESADO` aparece como **categoría de peso** (`CategoriaPeso`) y como **estado**
> (`EstadoPesaje`). Son enums distintos pese a compartir nombre.
