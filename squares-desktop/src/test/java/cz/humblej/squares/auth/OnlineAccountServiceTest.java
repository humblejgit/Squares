package cz.humblej.squares.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.identity.client.HttpTransport;
import cz.humblej.identity.client.OidcConfiguration;
import cz.humblej.identity.client.TokenSet;
import cz.humblej.identity.client.TokenStore;
import cz.humblej.identity.desktop.OidcClient;
import cz.humblej.identity.model.InstallationInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OnlineAccountServiceTest {
    private final Instant now = Instant.parse("2026-07-27T12:00:00Z");
    private HttpServer server;
    private URI base;
    private AtomicInteger tokenRequests;
    private AtomicInteger meRequests;
    private AtomicInteger profileRequests;
    private AtomicInteger installationRequests;
    private volatile boolean rejectRefresh;
    private volatile boolean profileMissing;

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByName("127.0.0.1"), 0), 0);
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        tokenRequests = new AtomicInteger();
        meRequests = new AtomicInteger();
        profileRequests = new AtomicInteger();
        installationRequests = new AtomicInteger();

        server.createContext("/issuer/.well-known/openid-configuration", exchange ->
                json(exchange, 200, "{"
                        + "\"issuer\":\"" + base + "/issuer\","
                        + "\"authorization_endpoint\":\"" + base + "/authorize\","
                        + "\"token_endpoint\":\"" + base + "/token\""
                        + "}"));
        server.createContext("/token", exchange -> {
            tokenRequests.incrementAndGet();
            String request = read(exchange.getRequestBody());
            if (!request.contains("grant_type=refresh_token")
                    || !request.contains("refresh_token=refresh")) {
                json(exchange, 400, "{\"error\":\"invalid_request\"}");
            } else if (rejectRefresh) {
                json(exchange, 400, "{\"error\":\"invalid_grant\"}");
            } else {
                json(exchange, 200, "{"
                        + "\"access_token\":\"fresh-access\","
                        + "\"refresh_token\":\"rotated-refresh\","
                        + "\"expires_in\":900"
                        + "}");
            }
        });
        server.createContext("/api/v1/me", exchange -> {
            if (exchange.getRequestURI().getPath().startsWith("/api/v1/me/installations/")) {
                installationRequests.incrementAndGet();
                String installationId = exchange.getRequestURI().getPath()
                        .substring("/api/v1/me/installations/".length());
                String request = read(exchange.getRequestBody());
                if (!"PUT".equals(exchange.getRequestMethod())
                        || !request.contains("\"platform\":\"WINDOWS\"")
                        || !request.contains("\"appVersion\":\"4.4.0\"")) {
                    json(exchange, 400, "{\"code\":\"invalid-request\"}");
                    return;
                }
                json(exchange, 200, "{\"installationId\":\"" + installationId + "\"}");
                return;
            }
            if ("/api/v1/me/profile".equals(exchange.getRequestURI().getPath())) {
                profileRequests.incrementAndGet();
                if ("GET".equals(exchange.getRequestMethod())) {
                    if (profileMissing) {
                        json(exchange, 404, "{\"code\":\"not-found\"}");
                        return;
                    }
                    json(exchange, 200, "{"
                            + "\"playerId\":\"00000000-0000-0000-0000-000000000001\","
                            + "\"handle\":\"tester\","
                            + "\"displayName\":\"Tester\","
                            + "\"revision\":1"
                            + "}");
                    return;
                }
                String request = read(exchange.getRequestBody());
                if (!"PUT".equals(exchange.getRequestMethod())
                        || !"Bearer fresh-access".equals(
                        exchange.getRequestHeaders().getFirst("Authorization"))
                        || !request.contains("\"handle\":\"new_handle\"")
                        || !request.contains("\"displayName\":\"Nový hráč\"")) {
                    json(exchange, 400, "{\"code\":\"invalid-request\"}");
                    return;
                }
                json(exchange, 200, "{"
                        + "\"playerId\":\"00000000-0000-0000-0000-000000000001\","
                        + "\"handle\":\"new_handle\","
                        + "\"displayName\":\"Nový hráč\","
                        + "\"revision\":2"
                        + "}");
                return;
            }
            meRequests.incrementAndGet();
            if (!"Bearer fresh-access".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                json(exchange, 401, "{\"code\":\"unauthorized\"}");
            } else {
                json(exchange, 200, "{"
                        + "\"accountStatus\":\"ACTIVE\","
                        + "\"playerId\":\"00000000-0000-0000-0000-000000000001\","
                        + "\"createdAt\":\"2026-07-27T12:00:00Z\""
                        + "}");
            }
        });
        server.start();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    @Test
    public void refreshesAndRetriesAfterUnauthorizedResponse() throws Exception {
        MemoryTokenStore store = new MemoryTokenStore(new TokenSet(
                "stale-access", "refresh", null, now.plusSeconds(600)));
        OnlineAccountService service = service(store);

        OnlineAccount account = service.getMe();

        assertEquals("tester", account.player().handle());
        assertEquals(1, tokenRequests.get());
        assertEquals(2, meRequests.get());
        assertEquals("fresh-access", store.tokens.getAccessToken());
        assertEquals("rotated-refresh", store.tokens.getRefreshToken());
    }

    @Test
    public void clearsSessionWhenRefreshTokenIsRejected() throws Exception {
        rejectRefresh = true;
        MemoryTokenStore store = new MemoryTokenStore(new TokenSet(
                "expired-access", "refresh", null, now.minusSeconds(1)));
        OnlineAccountService service = service(store);

        try {
            service.getMe();
            fail("Rejected refresh token must expire the session.");
        } catch (AuthenticationException exception) {
            assertTrue(exception.sessionExpired());
        }

        assertFalse(service.hasSession());
        assertTrue(store.cleared);
        assertEquals(0, meRequests.get());
    }

    @Test
    public void treatsMissingSquaresProfileAsOnboardingRequired() throws Exception {
        profileMissing = true;
        MemoryTokenStore store = new MemoryTokenStore(new TokenSet(
                "fresh-access", "refresh", null, now.plusSeconds(600)));
        OnlineAccountService service = service(store);

        OnlineAccount account = service.getMe();

        assertTrue(account.onboardingRequired());
        assertNull(account.player());
        assertEquals(1, meRequests.get());
        assertEquals(1, profileRequests.get());
    }

    @Test
    public void updatesProfileThroughAuthenticatedPut() throws Exception {
        MemoryTokenStore store = new MemoryTokenStore(new TokenSet(
                "fresh-access", "refresh", null, now.plusSeconds(600)));
        OnlineAccountService service = service(store);

        OnlinePlayer player = service.putProfile("new_handle", " Nový hráč ");

        assertEquals("new_handle", player.handle());
        assertEquals("Nový hráč", player.displayName());
        assertEquals(2, player.revision());
        assertEquals(1, profileRequests.get());
        assertEquals(0, tokenRequests.get());
    }

    @Test
    public void registersStableInstallationThroughAuthenticatedPut() throws Exception {
        MemoryTokenStore store = new MemoryTokenStore(new TokenSet(
                "fresh-access", "refresh", null, now.plusSeconds(600)));
        OnlineAccountService service = service(store);
        java.util.UUID installationId = java.util.UUID.randomUUID();

        service.registerInstallation(new InstallationInfo(
                installationId, "WINDOWS", "4.4.0",
                "4.4.0", "cs-CZ"));

        assertEquals(1, installationRequests.get());
        assertEquals(0, tokenRequests.get());
    }

    private OnlineAccountService service(MemoryTokenStore store) {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        OidcConfiguration configuration = new OidcConfiguration(
                URI.create(base + "/issuer"),
                URI.create(base + "/api/v1"),
                "squares-desktop");
        HttpTransport transport = new HttpTransport();
        return new OnlineAccountService(
                configuration,
                new OidcClient(configuration, clock),
                store,
                transport,
                new ObjectMapper(),
                clock);
    }

    private static String read(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void json(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static final class MemoryTokenStore implements TokenStore {
        private TokenSet tokens;
        private boolean cleared;

        private MemoryTokenStore(TokenSet tokens) {
            this.tokens = tokens;
        }

        @Override
        public TokenSet load() {
            return tokens;
        }

        @Override
        public void save(TokenSet tokens) {
            this.tokens = tokens;
        }

        @Override
        public void clear() {
            tokens = null;
            cleared = true;
        }
    }
}
