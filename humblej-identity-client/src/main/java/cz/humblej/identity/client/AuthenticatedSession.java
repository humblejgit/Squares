package cz.humblej.identity.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;

/**
 * Reusable authenticated HTTP session with persisted OIDC tokens, refresh and one retry after 401.
 */
public final class AuthenticatedSession {
    private static final long REFRESH_SKEW_SECONDS = 60;

    private final OidcConfiguration configuration;
    private final OidcTokenClient tokenClient;
    private final TokenStore tokenStore;
    private final HttpTransport transport;
    private final ObjectMapper mapper;
    private final Clock clock;

    private TokenSet tokens;
    private String restorationWarning;

    public AuthenticatedSession(
            OidcConfiguration configuration,
            OidcTokenClient tokenClient,
            TokenStore tokenStore,
            HttpTransport transport,
            ObjectMapper mapper,
            Clock clock) {
        this.configuration = configuration;
        this.tokenClient = tokenClient;
        this.tokenStore = tokenStore;
        this.transport = transport;
        this.mapper = mapper;
        this.clock = clock;
        try {
            this.tokens = tokenStore.load();
        } catch (IOException exception) {
            this.restorationWarning = exception.getMessage();
        }
    }

    public synchronized boolean hasSession() {
        return tokens != null;
    }

    public synchronized String consumeRestorationWarning() {
        String warning = restorationWarning;
        restorationWarning = null;
        return warning;
    }

    public void login() throws AuthenticationException {
        TokenSet newTokens = tokenClient.login();
        synchronized (this) {
            saveTokens(newTokens);
        }
    }

    public HttpTransport.Response get(String path) throws AuthenticationException {
        return request("GET", path, null, false, false);
    }

    public HttpTransport.Response get(String path, boolean allowNotFound)
            throws AuthenticationException {
        return request("GET", path, null, false, allowNotFound);
    }

    public HttpTransport.Response putJson(String path, String json)
            throws AuthenticationException {
        return request("PUT", path, json, false, false);
    }

    public void logout() {
        TokenSet current;
        synchronized (this) {
            current = tokens;
            tokens = null;
        }
        tokenClient.revoke(current);
        try {
            tokenStore.clear();
        } catch (IOException ignored) {
            // The in-memory session is already gone and the refresh token was revoked.
        }
    }

    private HttpTransport.Response request(
            String method, String path, String json,
            boolean alreadyRetried, boolean allowNotFound)
            throws AuthenticationException {
        TokenSet usable = usableTokens(false);
        HttpTransport.Response response;
        try {
            if (!OidcConfiguration.isSecureOrLoopback(configuration.apiBaseUri())) {
                throw new AuthenticationException(
                        "Identity API must use HTTPS outside local development.");
            }
            URI uri = URI.create(configuration.apiBaseUri().toString() + path);
            response = "PUT".equals(method)
                    ? transport.putJson(uri, json, usable.getAccessToken())
                    : transport.get(uri, usable.getAccessToken());
        } catch (IOException exception) {
            throw new AuthenticationException("Identity server is unavailable.", exception);
        }

        if (response.status() == 401 && !alreadyRetried) {
            usableTokens(true);
            return request(method, path, json, true, allowNotFound);
        }
        if (allowNotFound && response.status() == 404) {
            return response;
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw apiError(response);
        }
        return response;
    }

    private synchronized TokenSet usableTokens(boolean forceRefresh)
            throws AuthenticationException {
        if (tokens == null) {
            throw new AuthenticationException("No authenticated session is available.", null, true);
        }
        if (!forceRefresh && !tokens.expiresWithin(clock, REFRESH_SKEW_SECONDS)) {
            return tokens;
        }
        try {
            TokenSet refreshed = tokenClient.refresh(tokens);
            saveTokens(refreshed);
            return refreshed;
        } catch (AuthenticationException exception) {
            if (exception.sessionExpired()) {
                clearSession();
            }
            throw exception;
        }
    }

    private void saveTokens(TokenSet newTokens) throws AuthenticationException {
        try {
            tokenStore.save(newTokens);
            tokens = newTokens;
        } catch (IOException exception) {
            tokens = null;
            tokenClient.revoke(newTokens);
            throw new AuthenticationException(
                    "The authenticated session could not be stored securely and was ended.",
                    exception);
        }
    }

    private synchronized void clearSession() {
        tokens = null;
        try {
            tokenStore.clear();
        } catch (IOException ignored) {
            // A stale persisted file cannot authorize requests without a successful load.
        }
    }

    private AuthenticationException apiError(HttpTransport.Response response) {
        String detail = null;
        String code = null;
        try {
            JsonNode problem = mapper.readTree(response.body());
            detail = text(problem, "detail");
            code = text(problem, "code");
        } catch (IOException ignored) {
            // Fall back to the HTTP status.
        }
        StringBuilder message = new StringBuilder("Identity server returned HTTP ")
                .append(response.status());
        if (detail != null && !detail.trim().isEmpty()) {
            message.append(": ").append(detail);
        }
        if (code != null && !code.trim().isEmpty()) {
            message.append(" (").append(code).append(')');
        }
        return new AuthenticationException(message.append('.').toString());
    }

    private static String text(JsonNode json, String field) {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
