package cz.humblej.squares.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cz.humblej.identity.client.AuthenticatedSession;
import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.identity.client.HttpTransport;
import cz.humblej.identity.client.OidcConfiguration;
import cz.humblej.identity.client.TokenStore;
import cz.humblej.identity.desktop.DpapiTokenStore;
import cz.humblej.identity.desktop.OidcClient;
import cz.humblej.identity.model.InstallationInfo;
import cz.humblej.squares.app.BuildInfo;
import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerResult;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
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

    public GameSubmissionStatus submitGame(
            GameResult game, PlayerResult.Seat submittedBySeat,
            UUID playerId, UUID installationId) throws AuthenticationException {
        PlayerResult submitter = submittedBySeat == PlayerResult.Seat.RED
                ? game.redPlayer() : game.bluePlayer();
        if (submitter.playerType() != PlayerResult.PlayerType.PROFILE) {
            throw new AuthenticationException(
                    "Výsledek nelze odeslat za hráče bez místního profilu.");
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("rulesVersion", 1);
        body.put("coreVersion", BuildInfo.buildId());
        body.put("mode", game.mode().name());
        body.put("finishReason", game.finishReason().name());
        body.put("startedAt", game.startedAt().toString());
        body.put("finishedAt", game.finishedAt().toString());
        body.put("rows", game.rows());
        body.put("columns", game.columns());
        body.put("thinkingTimeLimitSeconds", game.thinkingTimeLimitSeconds());
        body.put("totalSeconds", game.totalSeconds());
        body.put("randomInitialEdges", game.randomInitialEdges());
        if (game.cpuDifficulty() != null) {
            body.put("cpuDifficulty", game.cpuDifficulty().name());
        }
        body.put("submittedBySeat", submittedBySeat.name());
        ArrayNode players = body.putArray("players");
        addPlayer(players, game.redPlayer(), submittedBySeat, playerId);
        addPlayer(players, game.bluePlayer(), submittedBySeat, playerId);

        HttpTransport.Response response = session.putJson(
                "/me/game-submissions/" + game.gameId(), body.toString(),
                Collections.singletonMap(
                        "X-Squares-Installation-Id", installationId.toString()));
        return parseSubmissionStatus(response.body(), game.gameId());
    }

    public GameSubmissionStatus getGameSubmission(UUID gameId)
            throws AuthenticationException {
        return parseSubmissionStatus(
                session.get("/me/game-submissions/" + gameId).body(), gameId);
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

    private static void addPlayer(ArrayNode players, PlayerResult player,
                                  PlayerResult.Seat submittedBySeat, UUID playerId) {
        ObjectNode json = players.addObject();
        json.put("seat", player.seat().name());
        json.put("playerType", player.playerType().name());
        if (player.seat() == submittedBySeat) {
            json.put("playerId", playerId.toString());
        }
        json.put("displayNameSnapshot", player.displayName());
        json.put("score", player.score());
        json.put("thinkingSeconds", player.thinkingSeconds());
        json.put("outcome", player.outcome().name());
    }

    private GameSubmissionStatus parseSubmissionStatus(byte[] body, UUID expectedGameId)
            throws AuthenticationException {
        try {
            JsonNode json = mapper.readTree(body);
            UUID gameId = UUID.fromString(requiredTextUnchecked(json, "gameId"));
            if (!expectedGameId.equals(gameId)) {
                throw new IllegalArgumentException("Game ID mismatch");
            }
            return new GameSubmissionStatus(
                    gameId,
                    requiredTextUnchecked(json, "submissionStatus"),
                    requiredTextUnchecked(json, "verificationStatus"),
                    json.path("ranked").asBoolean(),
                    Instant.parse(requiredTextUnchecked(json, "receivedAt")),
                    Instant.parse(requiredTextUnchecked(json, "updatedAt")));
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException(
                    "Squares server vrátil neplatný stav synchronizace.", exception);
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
