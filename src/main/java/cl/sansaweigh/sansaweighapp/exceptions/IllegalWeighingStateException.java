package cl.sansaweigh.sansaweighapp.exceptions;

public class IllegalWeighingStateException extends RuntimeException {
    public IllegalWeighingStateException(String message) {
        super(message);
    }
}
