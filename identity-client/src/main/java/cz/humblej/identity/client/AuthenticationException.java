package cz.humblej.identity.client;

public class AuthenticationException extends Exception {
    private final boolean sessionExpired;
    private final int httpStatus;
    private final String problemCode;

    public AuthenticationException(String message) {
        this(message, null, false);
    }

    public AuthenticationException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public AuthenticationException(String message, Throwable cause, boolean sessionExpired) {
        this(message, cause, sessionExpired, 0, null);
    }

    public AuthenticationException(String message, Throwable cause, boolean sessionExpired,
                                   int httpStatus, String problemCode) {
        super(message, cause);
        this.sessionExpired = sessionExpired;
        this.httpStatus = httpStatus;
        this.problemCode = problemCode;
    }

    public boolean sessionExpired() {
        return sessionExpired;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String problemCode() {
        return problemCode;
    }
}
