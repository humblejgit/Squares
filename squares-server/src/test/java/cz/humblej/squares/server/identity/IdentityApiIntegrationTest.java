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
        registry.add("squares.security.oidc.issuer", () -> ISSUER);
        registry.add("squares.security.oidc.audience", () -> "squares-api");
        registry.add("squares.security.oidc.jwk-set-uri", () -> "https://identity.squares.test/jwks");
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
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void createsAccountAndIdentityOnFirstAuthenticatedRequest() throws Exception {
        String subject = unique("first-login");

        mockMvc.perform(get("/api/v1/me").with(identity(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.onboardingRequired").value(true))
                .andExpect(jsonPath("$.player").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertEquals(1, count("""
                SELECT count(*)
                FROM account_identities
                WHERE issuer = ? AND subject = ?
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

        mockMvc.perform(get("/api/v1/me").with(identity(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingRequired").value(false))
                .andExpect(jsonPath("$.player.handle").value(handle));
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
