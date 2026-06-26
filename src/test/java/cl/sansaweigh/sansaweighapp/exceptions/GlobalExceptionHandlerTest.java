package cl.sansaweigh.sansaweighapp.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void reglaDeNegocio_mapea400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleReglaNegocio(new BusinessRuleException("horario nocturno"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "horario nocturno");
    }

    @Test
    void argumentoInvalido_mapea400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleArgumentoInvalido(new IllegalArgumentException("peso negativo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "peso negativo");
    }

    @Test
    void apiExternaNoDisponible_mapea503() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleApiExterna(new ExternalScaleUnavailableException("API caída", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", 503);
    }
}
