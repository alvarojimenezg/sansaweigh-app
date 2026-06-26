package cl.sansaweigh.sansaweighapp.exceptions;

/**
 * Se lanza cuando la API externa de especificaciones de balanza no responde
 * tras agotar los reintentos. El servicio la captura para activar el fallback
 * (caché en Redis o spec por defecto con id "-1").
 */
public class ExternalScaleUnavailableException extends RuntimeException {
    public ExternalScaleUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
