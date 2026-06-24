package cl.sansaweigh.sansaweighapp.exceptions;

/**
 * Se lanza cuando no existe un registro de pesaje con el ID solicitado.
 * Se mapea a HTTP 404 en {@link GlobalExceptionHandler}.
 */
public class RegistroNoEncontradoException extends RuntimeException {
    public RegistroNoEncontradoException(String message) {
        super(message);
    }
}
