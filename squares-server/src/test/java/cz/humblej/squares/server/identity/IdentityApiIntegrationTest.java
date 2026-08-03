package cz.humblej.squares.server.identity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Import(IdentityApiIntegrationTest.TestJwtConfiguration.class)
class IdentityApiIntegrationTest {
    private static final String ISSUER = "https://identity.squares.test";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4-alpine")
            .withDatabaseName("squares")
            .withUsername("squares")
            .withPassword("squares-test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("humblej.identity.oidc.issuer", () -> ISSUER);
        registry.add("humblej.identity.oidc.audience", () -> "squares-api");
        registry.add("humblej.identity.oidc.jwk-set-uri", () -> "https://identity.squares.test/jwks");
    }

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Vyžadováno přihlášení"))
                .andExpect(jsonPath("$.detail").value("Je vyžadován platný přístupový token."))
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void localizesProblemResponseFromAcceptLanguage() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Accept-Language", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.detail").value("A valid access token is required."))
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void createsAccountAndIdentityOnFirstAuthenticatedRequest() throws Exception {
        String subject = unique("first-login");

        mockMvc.perform(get("/api/v1/me").with(identity(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.playerId").isNotEmpty())
                .andExpect(jsonPath("$.onboardingRequired").doesNotExist())
                .andExpect(jsonPath("$.player").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/me/profile").with(identity(subject)))
                .andExpect(status().isNotFound());

        assertEquals(1, count("""
                SELECT count(*)
                FROM account_identities
                WHERE issuer = ? AND subject = ?
                """, ISSUER, subject));
        assertEquals(1, count("""
                SELECT count(*)
                FROM player_identities pi
                JOIN account_identities ai ON ai.account_id = pi.account_id
                WHERE ai.issuer = ? AND ai.subject = ?
                """, ISSUER, subject));
    }

    @Test
    void profileEndpointCanProvisionIdentityDirectly() throws Exception {
        String subject = unique("profile-first");

        mockMvc.perform(get("/api/v1/me/profile").with(identity(subject)))
                .andExpect(status().isNotFound());

        assertEquals(1, count("""
                SELECT count(*)
                FROM player_identities pi
                JOIN account_identities ai ON ai.account_id = pi.account_id
                WHERE ai.issuer = ? AND ai.subject = ?
                """, ISSUER, subject));
    }

    @Test
    void concurrentFirstLoginCreatesExactlyOneAccount() throws Exception {
        String subject = unique("concurrent");
        int accountCountBefore = count("SELECT count(*) FROM accounts");
        int workers = 8;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);

        try {
            List<Future<Integer>> responses = java.util.stream.IntStream.range(0, workers)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        assertTrue(start.await(10, TimeUnit.SECONDS));
                        return mockMvc.perform(get("/api/v1/me").with(identity(subject)))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    }))
                    .toList();

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            for (Future<Integer> response : responses) {
                assertEquals(200, response.get(20, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(accountCountBefore + 1, count("SELECT count(*) FROM accounts"));
        assertEquals(1, count("""
                SELECT count(*)
                FROM account_identities
                WHERE issuer = ? AND subject = ?
                """, ISSUER, subject));
    }

    @Test
    void createsAndIdempotentlyReplacesOwnProfile() throws Exception {
        String subject = unique("profile");
        String handle = unique("player").substring(0, 20);
        String body = """
                {"handle":"%s","displayName":"Jan"}
                """.formatted(handle);

        mockMvc.perform(put("/api/v1/me/profile")
                        .with(identity(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.handle").value(handle))
                .andExpect(jsonPath("$.displayName").value("Jan"))
                .andExpect(jsonPath("$.revision").value(1));

        mockMvc.perform(put("/api/v1/me/profile")
                        .with(identity(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.revision").value(1));

        mockMvc.perform(get("/api/v1/me/profile").with(identity(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handle").value(handle));
    }

    @Test
    void returnsConflictWhenTwoAccountsChooseSameHandle() throws Exception {
        String handle = unique("shared").substring(0, 20);
        String firstSubject = unique("owner-a");
        String secondSubject = unique("owner-b");
        String body = """
                {"handle":"%s","displayName":"Shared name"}
                """.formatted(handle);

        mockMvc.perform(put("/api/v1/me/profile")
                        .with(identity(firstSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/me/profile")
                        .with(identity(secondSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("handle-unavailable"));

        assertEquals(1, count("""
                SELECT count(*)
                FROM account_identities
                WHERE issuer = ? AND subject = ?
                """, ISSUER, secondSubject));
    }

    @Test
    void createsAndUpdatesInstallationIdempotently() throws Exception {
        String subject = unique("installation");
        UUID installationId = UUID.randomUUID();
        String initial = """
                {"platform":"WINDOWS","appVersion":"4.4.0",
                 "coreVersion":"4.4.0","locale":"cs-CZ"}
                """;

        mockMvc.perform(put("/api/v1/me/installations/{installationId}", installationId)
                        .with(identity(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initial))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/me/installations/" + installationId))
                .andExpect(jsonPath("$.installationId").value(installationId.toString()))
                .andExpect(jsonPath("$.platform").value("WINDOWS"))
                .andExpect(jsonPath("$.appVersion").value("4.4.0"));

        String update = """
                {"platform":"WINDOWS","appVersion":"4.4.1",
                 "coreVersion":"4.4.0","locale":"en-US"}
                """;
        mockMvc.perform(put("/api/v1/me/installations/{installationId}", installationId)
                        .with(identity(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationId").value(installationId.toString()))
                .andExpect(jsonPath("$.appVersion").value("4.4.1"))
                .andExpect(jsonPath("$.locale").value("en-US"));

        assertEquals(1, count("SELECT count(*) FROM installations WHERE installation_id = ?",
                installationId));
        assertEquals(1, count("""
                SELECT count(*)
                FROM player_identities pi
                JOIN account_identities ai ON ai.account_id = pi.account_id
                WHERE ai.issuer = ? AND ai.subject = ?
                """, ISSUER, subject));
    }

    @Test
    void rejectsInvalidInstallationMetadata() throws Exception {
        String subject = unique("invalid-installation");
        UUID installationId = UUID.randomUUID();
        String invalid = """
                {"platform":"LINUX","appVersion":"snapshot",
                 "coreVersion":"4.4","locale":"x"}
                """;

        mockMvc.perform(put("/api/v1/me/installations/{installationId}", installationId)
                        .with(identity(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Neplatný požadavek"))
                .andExpect(jsonPath("$.detail").value("Požadavek obsahuje neplatná pole."))
                .andExpect(jsonPath("$.code").value("validation-failed"))
                .andExpect(jsonPath("$.violations").isArray());

        assertEquals(0, count(
                "SELECT count(*) FROM installations WHERE installation_id = ?",
                installationId));
    }

    @Test
    void storesGameSubmissionIdempotentlyAndReturnsItsStatus() throws Exception {
        String subject = unique("submission");
        UUID playerId = resolvePlayerId(subject);
        UUID installationId = registerInstallation(subject);
        UUID gameId = UUID.randomUUID();
        String body = localSubmission(playerId, 8);

        mockMvc.perform(put("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(subject))
                        .header("X-Squares-Installation-Id", installationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()))
                .andExpect(jsonPath("$.submissionStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.ranked").value(false));

        mockMvc.perform(put("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(subject))
                        .header("X-Squares-Installation-Id", installationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionStatus").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionStatus").value("ACCEPTED"));

        assertEquals(1, count("SELECT count(*) FROM game_submissions WHERE game_id = ?", gameId));
        assertEquals(1, count("SELECT count(*) FROM games WHERE game_id = ?", gameId));
    }

    @Test
    void rejectsDifferentPayloadForSamePlayerAndGame() throws Exception {
        String subject = unique("submission-conflict");
        UUID playerId = resolvePlayerId(subject);
        UUID installationId = registerInstallation(subject);
        UUID gameId = UUID.randomUUID();

        submit(subject, installationId, gameId, localSubmission(playerId, 8), 201);
        mockMvc.perform(put("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(subject))
                        .header("X-Squares-Installation-Id", installationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(localSubmission(playerId, 9)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("submission-payload-conflict"));
    }

    @Test
    void matchesEqualNetworkSubmissionsFromBothPlayers() throws Exception {
        String redSubject = unique("network-red");
        String blueSubject = unique("network-blue");
        UUID redPlayerId = resolvePlayerId(redSubject);
        UUID bluePlayerId = resolvePlayerId(blueSubject);
        UUID redInstallation = registerInstallation(redSubject);
        UUID blueInstallation = registerInstallation(blueSubject);
        UUID gameId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(redSubject))
                        .header("X-Squares-Installation-Id", redInstallation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(networkSubmission("RED", redPlayerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionStatus").value("PENDING_PEER"));

        mockMvc.perform(put("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(blueSubject))
                        .header("X-Squares-Installation-Id", blueInstallation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(networkSubmission("BLUE", bluePlayerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionStatus").value("MATCHED"))
                .andExpect(jsonPath("$.verificationStatus").value("PEER_CONFIRMED"));

        mockMvc.perform(get("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(redSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionStatus").value("MATCHED"))
                .andExpect(jsonPath("$.verificationStatus").value("PEER_CONFIRMED"));
    }

    @Test
    void rejectsUnregisteredInstallationAndInvalidDomainResult() throws Exception {
        String subject = unique("invalid-submission");
        UUID playerId = resolvePlayerId(subject);
        UUID gameId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(subject))
                        .header("X-Squares-Installation-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(localSubmission(playerId, 8)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("installation-not-registered"));

        UUID installationId = registerInstallation(subject);
        String invalid = localSubmission(playerId, 8)
                .replace("\"outcome\":\"WIN\"", "\"outcome\":\"DRAW\"");
        mockMvc.perform(put("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(subject))
                        .header("X-Squares-Installation-Id", installationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("invalid-game-submission"));
    }

    private UUID resolvePlayerId(String subject) throws Exception {
        mockMvc.perform(get("/api/v1/me").with(identity(subject)))
                .andExpect(status().isOk());
        return jdbc.queryForObject("""
                SELECT pi.player_id
                FROM player_identities pi
                JOIN account_identities ai ON ai.account_id = pi.account_id
                WHERE ai.issuer = ? AND ai.subject = ?
                """, UUID.class, ISSUER, subject);
    }

    private UUID registerInstallation(String subject) throws Exception {
        UUID installationId = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/me/installations/{installationId}", installationId)
                        .with(identity(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"platform":"WINDOWS","appVersion":"4.5.0",
                                 "coreVersion":"4.5.0","locale":"cs-CZ"}
                                """))
                .andExpect(status().isCreated());
        return installationId;
    }

    private void submit(String subject, UUID installationId, UUID gameId,
                        String body, int expectedStatus) throws Exception {
        mockMvc.perform(put("/api/v1/me/game-submissions/{gameId}", gameId)
                        .with(identity(subject))
                        .header("X-Squares-Installation-Id", installationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    private static String localSubmission(UUID playerId, int redScore) {
        return """
                {
                  "rulesVersion":1,"coreVersion":"4.5.0","mode":"LOCAL",
                  "finishReason":"BOARD_FULL","startedAt":"2026-08-03T12:00:00Z",
                  "finishedAt":"2026-08-03T12:02:00Z","rows":5,"columns":5,
                  "thinkingTimeLimitSeconds":120,"totalSeconds":120,
                  "randomInitialEdges":false,"submittedBySeat":"RED",
                  "players":[
                    {"seat":"RED","playerType":"PROFILE","playerId":"%s",
                     "displayNameSnapshot":"Red","score":%d,"thinkingSeconds":50,"outcome":"WIN"},
                    {"seat":"BLUE","playerType":"GUEST","displayNameSnapshot":"Blue",
                     "score":4,"thinkingSeconds":60,"outcome":"LOSS"}
                  ]
                }
                """.formatted(playerId, redScore);
    }

    private static String networkSubmission(String seat, UUID playerId) {
        String redId = "RED".equals(seat) ? ",\"playerId\":\"" + playerId + "\"" : "";
        String blueId = "BLUE".equals(seat) ? ",\"playerId\":\"" + playerId + "\"" : "";
        return """
                {
                  "rulesVersion":1,"coreVersion":"4.5.0","mode":"NETWORK",
                  "finishReason":"BOARD_FULL","startedAt":"2026-08-03T12:00:00Z",
                  "finishedAt":"2026-08-03T12:02:10Z","rows":5,"columns":5,
                  "thinkingTimeLimitSeconds":120,"totalSeconds":130,
                  "randomInitialEdges":false,"submittedBySeat":"%s",
                  "players":[
                    {"seat":"RED","playerType":"PROFILE"%s,
                     "displayNameSnapshot":"Red","score":8,"thinkingSeconds":60,"outcome":"WIN"},
                    {"seat":"BLUE","playerType":"PROFILE"%s,
                     "displayNameSnapshot":"Blue","score":4,"thinkingSeconds":70,"outcome":"LOSS"}
                  ]
                }
                """.formatted(seat, redId, blueId);
    }

    private RequestPostProcessor identity(String subject) {
        return jwt().jwt(token -> token
                .issuer(ISSUER)
                .subject(subject)
                .audience(List.of("squares-api")));
    }

    private int count(String sql, Object... parameters) {
        Integer result = jdbc.queryForObject(sql, Integer.class, parameters);
        return result == null ? 0 : result;
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestJwtConfiguration {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new JwtException("The test uses authenticated MockMvc JWT requests.");
            };
        }
    }
}
