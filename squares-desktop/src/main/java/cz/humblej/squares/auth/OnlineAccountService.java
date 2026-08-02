package cz.humblej.squares.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.humblej.identity.client.AuthenticatedSession;
import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.identity.client.HttpTransport;
import cz.humblej.identity.client.OidcConfiguration;
import cz.humblej.identity.client.TokenStore;
import cz.humblej.identity.desktop.DpapiTokenStore;
import cz.humblej.identity.desktop.OidcClient;
import cz.humblej.identity.model.InstallationInfo;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.UUID;

/** Squares-specific mapping built on the reusable authenticated identity session. */
public final class OnlineAccountService {
    private final AuthenticatedSession session;
    private final ObjectMapper mapper;

    OnlineAccountService(
            OidcConfiguration configuration,
            OidcClient oidcClient,
            TokenStore tokenStore,
            HttpTransport transport,
            ObjectMapper mapper,
            Clock clock) {
        this.mapper = mapper;
        this.session = new AuthenticatedSession(
                configuration, oidcClient, tokenStore, transport, mapper, clock);
    }

    public static OnlineAccountService systemDefault() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path dataDirectory = localAppData == null || localAppData.trim().isEmpty()
                ? Paths.get(System.getProperty("user.home"), ".squares")
                : Paths.get(localAppData, "Squares");
        OidcConfiguration configuration = new OidcConfiguration(
                URI.create(setting("squares.oidc.issuer", "SQUARES_DESKTOP_OIDC_ISSUER",
                        "http://localhost:9090/realms/squares")),
                URI.create(setting("squares.api.base-uri", "SQUARES_DESKTOP_API_BASE_URI",
                        "http://localhost:8080/api/v1")),
                setting("squares.oidc.client-id", "SQUARES_DESKTOP_OIDC_CLIENT_ID",
                        "squares-desktop"));
        Clock clock = Clock.systemUTC();
        ObjectMapper mapper = new ObjectMapper();
        return new OnlineAccountService(
                configuration,
                new OidcClient(configuration, clock),
                new DpapiTokenStore(dataDirectory.resolve("oidc-session.dat")),
                new HttpTransport(),
                mapper,
                clock);
    }

    public boolean hasSession() {
        return session.hasSession();
    }

    public String consumeRestorationWarning() {
        return session.consumeRestorationWarning();
    }

    public OnlineAccount login() throws AuthenticationException {
        session.login();
        return getMe();
    }

    public OnlineAccount getMe() throws AuthenticationException {
        HttpTransport.Response response = session.get("/me");
        HttpTransport.Response profileResponse = session.get("/me/profile", true);
        OnlinePlayer player = profileResponse.status() == 404
                ? null : parsePlayer(profileResponse.body());
        return parseAccount(response, player);
    }

    public OnlinePlayer putProfile(String handle, String displayName)
            throws AuthenticationException {
        if (handle == null || !handle.matches("^[a-z0-9][a-z0-9_-]{2,23}$")) {
            throw new AuthenticationException(
                    "Uživatelské jméno musí mít 3–24 znaků, začínat písmenem nebo číslem "
                            + "a obsahovat jen a–z, 0–9, _ nebo -.");
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
            throw new AuthenticationException(
                    "Profil se nepodařilo připravit k odeslání.", exception);
        }

        return parsePlayer(session.putJson("/me/profile", body).body());
    }

    public void registerInstallation(InstallationInfo installation)
            throws AuthenticationException {
        String body;
        try {
            body = mapper.createObjectNode()
                    .put("platform", installation.platform())
                    .put("appVersion", installation.appVersion())
                    .put("coreVersion", installation.coreVersion())
                    .put("locale", installation.locale())
                    .toString();
        } catch (RuntimeException exception) {
            throw new AuthenticationException(
                    "Registraci instalace se nepodařilo připravit.", exception);
        }

        HttpTransport.Response response = session.putJson(
                "/me/installations/" + installation.installationId(), body);
        try {
            JsonNode json = mapper.readTree(response.body());
            UUID returned = UUID.fromString(requiredTextUnchecked(json, "installationId"));
            if (!installation.installationId().equals(returned)) {
                throw new IllegalArgumentException("Installation ID mismatch");
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException(
                    "Squares server vrátil neplatnou registraci instalace.", exception);
        }
    }

    public void logout() {
        session.logout();
    }

    private OnlineAccount parseAccount(
            HttpTransport.Response response, OnlinePlayer player)
            throws AuthenticationException {
        try {
            JsonNode json = mapper.readTree(response.body());
            return new OnlineAccount(
                    requiredText(json, "accountStatus"),
                    UUID.fromString(requiredText(json, "playerId")),
                    player == null,
                    player);
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException(
                    "Squares server vrátil neplatná data účtu.", exception);
        }
    }

    private OnlinePlayer parsePlayer(byte[] body) throws AuthenticationException {
        try {
            return parsePlayer(mapper.readTree(body));
        } catch (IOException exception) {
            throw new AuthenticationException(
                    "Squares server vrátil neplatná data profilu.", exception);
        }
    }

    private static OnlinePlayer parsePlayer(JsonNode json) {
        return new OnlinePlayer(
                requiredTextUnchecked(json, "playerId"),
                requiredTextUnchecked(json, "handle"),
                requiredTextUnchecked(json, "displayName"),
                json.path("revision").asLong());
    }

    private static String requiredText(JsonNode json, String field)
            throws AuthenticationException {
        String value = text(json, field);
        if (value == null || value.trim().isEmpty()) {
            throw new AuthenticationException(
                    "Odpověď serveru neobsahuje pole " + field + ".");
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

    private static String setting(String property, String environment, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(environment);
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
