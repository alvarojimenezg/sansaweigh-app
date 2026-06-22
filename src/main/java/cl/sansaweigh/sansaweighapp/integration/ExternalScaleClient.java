package cl.sansaweigh.sansaweighapp.integration;

import cl.sansaweigh.sansaweighapp.dto.ScaleSpecificationDTO;
import cl.sansaweigh.sansaweighapp.exceptions.ExternalScaleUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP hacia la API externa de especificaciones de balanza (simulada con Mockoon).
 * La URL base se inyecta desde application.properties (external.scale.api.url).
 *
 * Implementa reintentos con backoff exponencial: ante un fallo transitorio de red reintenta
 * hasta MAX_ATTEMPTS veces, esperando cada vez más (500ms, 1000ms). Si agota los intentos,
 * lanza ExternalScaleUnavailableException para que el servicio active el fallback.
 */
@Slf4j
@Component
public class ExternalScaleClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 500;

    private final RestClient restClient;

    public ExternalScaleClient(@Value("${external.scale.api.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ScaleSpecificationDTO getScaleSpecifications(String scaleId) {
        RestClientException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return restClient.get()
                        .uri("/scales/{scaleId}", scaleId)
                        .retrieve()
                        .body(ScaleSpecificationDTO.class);
            } catch (RestClientException ex) {
                lastError = ex;
                log.warn("Intento {}/{} fallido al consultar specs de balanza {}: {}",
                        attempt, MAX_ATTEMPTS, scaleId, ex.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    // Backoff exponencial: 500ms, luego 1000ms, ...
                    sleep(BASE_BACKOFF_MILLIS * (1L << (attempt - 1)));
                }
            }
        }

        throw new ExternalScaleUnavailableException(
                "No se pudo obtener specs de la balanza " + scaleId + " tras " + MAX_ATTEMPTS + " intentos",
                lastError);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalScaleUnavailableException("Reintento interrumpido", e);
        }
    }
}
