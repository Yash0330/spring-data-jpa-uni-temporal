package dev.yash.jpatemporal.exception;

public class JpaUniTemporalException extends RuntimeException {

    public JpaUniTemporalException(final String message) {
        super(message);
    }

    public JpaUniTemporalException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
