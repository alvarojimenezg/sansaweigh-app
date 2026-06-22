package cl.sansaweigh.sansaweighapp.services;

import cl.sansaweigh.sansaweighapp.entities.ScaleSpecification;

public interface ScaleSpecificationService {

    /**
     * Obtiene las especificaciones de una balanza aplicando la estrategia de caché + fallback:
     * 1. Cache hit: si están en Redis, las devuelve directamente.
     * 2. Cache miss: consulta la API externa y cachea el resultado (TTL 120s).
     * 3. Fallback: si la API falla tras los reintentos, devuelve la spec por defecto (id "-1").
     */
    ScaleSpecification getScaleSpecification(String scaleId);

    /**
     * Devuelve la spec por defecto (id "-1"), creándola en Redis si no existe.
     * Usada como último recurso del fallback y para el seed inicial.
     */
    ScaleSpecification getDefaultSpecification();
}
