package cz.humblej.identity.client;

public class AuthenticationException extends Exception {
    private final boolean sessionExpired;

    public AuthenticationException(String message) {
        this(message, null, false);
    }

    public AuthenticationException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public AuthenticationException(String message, Throwable cause, boolean sessionExpired) {
        super(message, cause);
        this.sessionExpired = sessionExpired;
    }

    public boolean sessionExpired() {
        return sessionExpired;
    }
}
