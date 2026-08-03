package cz.humblej.identity.server;

/** Stable keys for messages exposed by the identity REST API. */
public final class IdentityServerMessageKeys {
    public static final String UNAUTHORIZED_TITLE = "identity.api.unauthorized.title";
    public static final String UNAUTHORIZED_DETAIL = "identity.api.unauthorized.detail";
    public static final String FORBIDDEN_TITLE = "identity.api.forbidden.title";
    public static final String FORBIDDEN_DETAIL = "identity.api.forbidden.detail";
    public static final String INVALID_REQUEST_TITLE = "identity.api.invalid-request.title";
    public static final String INVALID_REQUEST_DETAIL = "identity.api.invalid-request.detail";
    public static final String HANDLE_UNAVAILABLE_TITLE = "identity.api.handle-unavailable.title";
    public static final String HANDLE_UNAVAILABLE_DETAIL = "identity.api.handle-unavailable.detail";

    private IdentityServerMessageKeys() {
    }
}
