package cz.humblej.squares.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;

public final class OnlineAccountService {
    private static final long REFRESH_SKEW_SECONDS = 60;

    private final OidcConfiguration configuration;
    private final OidcClient oidcClient;
    private final TokenStore tokenStore;
    private final HttpTransport transport;
    private final ObjectMapper mapper;
    private final Clock clock;

    private TokenSet tokens;
    private String restorationWarning;

    OnlineAccountService(OidcConfiguration configuration, OidcClient oidcClient,
                         TokenStore tokenStore, HttpTransport transport,
                         ObjectMapper mapper, Clock clock) {
        this.configuration = configuration;
        this.oidcClient = oidcClient;
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

    public static OnlineAccountService systemDefault() {
        OidcConfiguration configuration = OidcConfiguration.systemDefault();
        Clock clock = Clock.systemUTC();
        ObjectMapper mapper = new ObjectMapper();
        return new OnlineAccountService(
                configuration,
                new OidcClient(configuration, clock),
                new DpapiTokenStore(configuration.tokenPath()),
                new HttpTransport(),
                mapper,
                clock);
    }

    public synchronized boolean hasSession() {
        return tokens != null;
    }

    public synchronized String consumeRestorationWarning() {
        String warning = restorationWarning;
        restorationWarning = null;
        return warning;
    }

    public OnlineAccount login() throws AuthenticationException {
        TokenSet newTokens = oidcClient.login();
        synchronized (this) {
            saveTokens(newTokens);
        }
        try {
            return getMe();
        } catch (AuthenticationException exception) {
            if (exception.sessionExpired()) {
                clearSession();
            }
            throw exception;
        }
    }

    public OnlineAccount getMe() throws AuthenticationException {
        HttpTransport.Response response = authorizedRequest("GET", "/me", null, false);
        return parseAccount(response);
    }

    public OnlinePlayer putProfile(String handle, String displayName) throws AuthenticationException {
        if (handle == null || !handle.matches("^[a-z0-9][a-z0-9_-]{2,23}$")) {
            throw new AuthenticationException(
                    "Uživatelské jméno musí mít 3–24 znaků, začínat písmenem nebo číslem a obsahovat jen a–z, 0–9, _ nebo -.");
        }
        String normalizedName = displayName == null ? "" : displayName.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 40) {
            throw new AuthenticationException("Zobrazované jméno musí mít 1–40 znaků.");
        }

        String body;
        try {
            body = mapper.createObjectNode()
                    .put("handle", handle)
                    .put("displayName", normalizedName)
                    .toString();
        } catch (RuntimeException exception) {
            throw new AuthenticationException("Profil se nepodařilo připravit k odeslání.", exception);
        }

        HttpTransport.Response response = authorizedRequest("PUT", "/me/profile", body, false);
        return parsePlayer(response.body());
    }

    public void logout() {
        TokenSet current;
        synchronized (this) {
            current = tokens;
            tokens = null;
        }
        oidcClient.revoke(current);
        try {
            tokenStore.clear();
        } catch (IOException ignored) {
            // The in-memory session is already gone; a future refresh will be rejected by revocation.
        }
    }

    private HttpTransport.Response authorizedRequest(String method, String path, String json,
                                                     boolean alreadyRetried)
            throws AuthenticationException {
        TokenSet usable = usableTokens(false);
        HttpTransport.Response response;
        try {
            if (!OidcConfiguration.isSecureOrLoopback(configuration.apiBaseUri())) {
                throw new AuthenticationException(
                        "Squares API musí používat HTTPS (mimo lokální vývoj).");
            }
            URI uri = URI.create(configuration.apiBaseUri().toString() + path);
            response = "PUT".equals(method)
                    ? transport.putJson(uri, json, usable.getAccessToken())
                    : transport.get(uri, usable.getAccessToken());
        } catch (IOException exception) {
            throw new AuthenticationException("Squares server není dostupný.", exception);
        }

        if (response.status() == 401 && !alreadyRetried) {
            usableTokens(true);
            return authorizedRequest(method, path, json, true);
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw apiError(response);
        }
        return response;
    }

    private synchronized TokenSet usableTokens(boolean forceRefresh) throws AuthenticationException {
        if (tokens == null) {
            throw new AuthenticationException("Nejste přihlášeni.", null, true);
        }
        if (!forceRefresh && !tokens.expiresWithin(clock, REFRESH_SKEW_SECONDS)) {
            return tokens;
        }

        try {
            TokenSet refreshed = oidcClient.refresh(tokens);
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
            oidcClient.revoke(newTokens);
            throw new AuthenticationException(
                    "Přihlášení se nepodařilo bezpečně uložit. Relace byla ukončena.", exception);
        }
    }

    private synchronized void clearSession() {
        tokens = null;
        try {
            tokenStore.clear();
        } catch (IOException ignored) {
            // A stale encrypted file cannot authorize requests without a successful load.
        }
    }

    private OnlineAccount parseAccount(HttpTransport.Response response) throws AuthenticationException {
        try {
            JsonNode json = mapper.readTree(response.body());
            JsonNode playerNode = json.get("player");
            OnlinePlayer player = playerNode == null || playerNode.isNull()
                    ? null : parsePlayer(playerNode);
            return new OnlineAccount(
                    requiredText(json, "accountStatus"),
                    json.path("onboardingRequired").asBoolean(),
                    player);
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException("Squares server vrátil neplatná data účtu.", exception);
        }
    }

    private OnlinePlayer parsePlayer(byte[] body) throws AuthenticationException {
        try {
            return parsePlayer(mapper.readTree(body));
        } catch (IOException exception) {
            throw new AuthenticationException("Squares server vrátil neplatná data profilu.", exception);
        }
    }

    private static OnlinePlayer parsePlayer(JsonNode json) {
        return new OnlinePlayer(
                requiredTextUnchecked(json, "playerId"),
                requiredTextUnchecked(json, "handle"),
                requiredTextUnchecked(json, "displayName"),
                json.path("revision").asLong());
    }

    private AuthenticationException apiError(HttpTransport.Response response) {
        String detail = null;
        String code = null;
        try {
            JsonNode problem = mapper.readTree(response.body());
            detail = text(problem, "detail");
            code = text(problem, "code");
        } catch (IOException ignored) {
            // Fall back to the HTTP status below.
        }
        StringBuilder message = new StringBuilder("Squares server vrátil HTTP ")
                .append(response.status());
        if (detail != null && !detail.trim().isEmpty()) {
            message.append(": ").append(detail);
        }
        if (code != null && !code.trim().isEmpty()) {
            message.append(" (").append(code).append(')');
        }
        return new AuthenticationException(message.toString() + ".");
    }

    private static String requiredText(JsonNode json, String field) throws AuthenticationException {
        String value = text(json, field);
        if (value == null || value.trim().isEmpty()) {
            throw new AuthenticationException("Odpověď serveru neobsahuje pole " + field + ".");
        }
        return value;
    }

    private static String requiredTextUnchecked(JsonNode json, String field) {
        String value = text(json, field);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value;
    }

    private static String text(JsonNode json, String field) {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
