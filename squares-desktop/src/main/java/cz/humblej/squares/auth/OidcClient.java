package cz.humblej.squares.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class OidcClient {
    private static final int LOGIN_TIMEOUT_SECONDS = 180;

    private final OidcConfiguration configuration;
    private final HttpTransport transport;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final SecureRandom random;
    private volatile ProviderMetadata metadata;

    OidcClient(OidcConfiguration configuration, Clock clock) {
        this(configuration, new HttpTransport(), new ObjectMapper(), clock, new SecureRandom());
    }

    OidcClient(OidcConfiguration configuration, HttpTransport transport, ObjectMapper mapper,
               Clock clock, SecureRandom random) {
        this.configuration = configuration;
        this.transport = transport;
        this.mapper = mapper;
        this.clock = clock;
        this.random = random;
    }

    TokenSet login() throws AuthenticationException {
        ProviderMetadata provider = metadata();
        String verifier = randomBase64Url(64);
        String state = randomBase64Url(32);
        String nonce = randomBase64Url(32);
        String challenge = sha256Base64Url(verifier);

        HttpServer callbackServer = null;
        try {
            callbackServer = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            callbackServer.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "squares-oidc-callback");
                thread.setDaemon(true);
                return thread;
            }));

            int port = callbackServer.getAddress().getPort();
            URI redirectUri = URI.create("http://127.0.0.1:" + port + "/oauth/callback");
            CountDownLatch callbackReceived = new CountDownLatch(1);
            AtomicReference<Callback> callback = new AtomicReference<>();
            callbackServer.createContext("/oauth/callback", exchange -> {
                Callback result = Callback.from(exchange.getRequestURI().getRawQuery());
                boolean validState = secureEquals(state, result.state);
                boolean complete = validState && (result.error != null || !isBlank(result.code));
                try {
                    sendBrowserResponse(exchange, validState && result.error == null && !isBlank(result.code));
                    if (complete) {
                        callback.compareAndSet(null, result);
                    }
                } finally {
                    if (complete) {
                        callbackReceived.countDown();
                    }
                }
            });
            callbackServer.start();

            URI authorizationUri = authorizationUri(provider.authorizationEndpoint, redirectUri,
                    challenge, state, nonce);
            openSystemBrowser(authorizationUri);

            if (!callbackReceived.await(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AuthenticationException("Přihlášení nebylo dokončeno do tří minut.");
            }
            Callback result = callback.get();
            if (result == null) {
                throw new AuthenticationException("Lokální přihlašovací callback nevrátil odpověď.");
            }
            if (!secureEquals(state, result.state)) {
                throw new AuthenticationException("Přihlašovací odpověď obsahuje neplatný parametr state.");
            }
            if (result.error != null) {
                throw new AuthenticationException("Poskytovatel přihlášení vrátil chybu: " + result.error);
            }
            if (isBlank(result.code)) {
                throw new AuthenticationException("Přihlašovací odpověď neobsahuje autorizační kód.");
            }

            String form = form(new String[][]{
                    {"grant_type", "authorization_code"},
                    {"client_id", configuration.clientId()},
                    {"code", result.code},
                    {"redirect_uri", redirectUri.toString()},
                    {"code_verifier", verifier}
            });
            return parseTokens(transport.postForm(provider.tokenEndpoint, form), null);
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("Přihlášení bylo přerušeno.", exception);
        } catch (IOException exception) {
            throw new AuthenticationException("Přihlášení se nepodařilo dokončit.", exception);
        } finally {
            if (callbackServer != null) {
                callbackServer.stop(0);
            }
        }
    }

    TokenSet refresh(TokenSet current) throws AuthenticationException {
        if (current == null || !current.canRefresh()) {
            throw new AuthenticationException("Přihlašovací relaci již nelze obnovit.", null, true);
        }
        ProviderMetadata provider = metadata();
        String form = form(new String[][]{
                {"grant_type", "refresh_token"},
                {"client_id", configuration.clientId()},
                {"refresh_token", current.getRefreshToken()}
        });
        try {
            return parseTokens(transport.postForm(provider.tokenEndpoint, form), current);
        } catch (IOException exception) {
            throw new AuthenticationException("Obnovení přihlášení selhalo.", exception);
        }
    }

    void revoke(TokenSet tokens) {
        if (tokens == null || !tokens.canRefresh()) {
            return;
        }
        try {
            ProviderMetadata provider = metadata();
            if (provider.revocationEndpoint == null) {
                return;
            }
            String form = form(new String[][]{
                    {"client_id", configuration.clientId()},
                    {"token", tokens.getRefreshToken()},
                    {"token_type_hint", "refresh_token"}
            });
            transport.postForm(provider.revocationEndpoint, form);
        } catch (Exception ignored) {
            // Local logout must succeed even when the provider is unavailable.
        }
    }

    private ProviderMetadata metadata() throws AuthenticationException {
        ProviderMetadata cached = metadata;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (metadata != null) {
                return metadata;
            }
            try {
                validateEndpoint(configuration.issuer());
                HttpTransport.Response response = transport.get(configuration.discoveryUri(), null);
                if (response.status() != 200) {
                    throw new AuthenticationException(
                            "OIDC discovery skončilo stavem HTTP " + response.status() + ".");
                }
                JsonNode json = mapper.readTree(response.body());
                URI discoveredIssuer = URI.create(requiredText(json, "issuer"));
                if (!configuration.issuer().equals(withoutTrailingSlash(discoveredIssuer))) {
                    throw new AuthenticationException("OIDC discovery vrátilo jiného vydavatele.");
                }
                URI authorization = secureEndpoint(json, "authorization_endpoint");
                URI token = secureEndpoint(json, "token_endpoint");
                URI revocation = optionalSecureEndpoint(json, "revocation_endpoint");
                metadata = new ProviderMetadata(authorization, token, revocation);
                return metadata;
            } catch (AuthenticationException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new AuthenticationException("Konfiguraci OIDC se nepodařilo načíst.", exception);
            }
        }
    }

    private TokenSet parseTokens(HttpTransport.Response response, TokenSet previous)
            throws AuthenticationException {
        try {
            if (response.status() < 200 || response.status() >= 300) {
                String oauthError = errorText(response.body());
                boolean expired = previous != null
                        && (response.status() == 400 || response.status() == 401);
                throw new AuthenticationException(
                        "Token endpoint odmítl požadavek"
                                + (oauthError == null ? "." : ": " + oauthError),
                        null, expired);
            }
            JsonNode json = mapper.readTree(response.body());
            String accessToken = requiredText(json, "access_token");
            String refreshToken = text(json, "refresh_token");
            if (isBlank(refreshToken) && previous != null) {
                refreshToken = previous.getRefreshToken();
            }
            String idToken = text(json, "id_token");
            if (isBlank(idToken) && previous != null) {
                idToken = previous.getIdToken();
            }
            long expiresIn = json.path("expires_in").asLong(0);
            if (expiresIn <= 0) {
                throw new AuthenticationException("Token endpoint nevrátil platnou dobu platnosti tokenu.");
            }
            return new TokenSet(accessToken, refreshToken, idToken, clock.instant().plusSeconds(expiresIn));
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException("Odpověď token endpointu není platná.", exception);
        }
    }

    private URI authorizationUri(URI endpoint, URI redirectUri, String challenge,
                                 String state, String nonce) {
        String query = form(new String[][]{
                {"response_type", "code"},
                {"client_id", configuration.clientId()},
                {"redirect_uri", redirectUri.toString()},
                {"scope", "openid"},
                {"code_challenge", challenge},
                {"code_challenge_method", "S256"},
                {"state", state},
                {"nonce", nonce}
        });
        return URI.create(endpoint.toString() + (endpoint.getQuery() == null ? "?" : "&") + query);
    }

    static String sha256Base64Url(String value) throws AuthenticationException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(value.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AuthenticationException("V systému není dostupný algoritmus SHA-256.", exception);
        }
    }

    private String randomBase64Url(int byteCount) {
        byte[] bytes = new byte[byteCount];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void openSystemBrowser(URI uri) throws AuthenticationException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new AuthenticationException("Systémový prohlížeč není dostupný.");
        }
        try {
            Desktop.getDesktop().browse(uri);
        } catch (IOException exception) {
            throw new AuthenticationException("Systémový prohlížeč se nepodařilo otevřít.", exception);
        }
    }

    private static void sendBrowserResponse(HttpExchange exchange, boolean success) throws IOException {
        String title = success ? "Přihlášení dokončeno" : "Přihlášení se nezdařilo";
        String html = "<!doctype html><html lang=\"cs\"><meta charset=\"utf-8\"><title>"
                + title + "</title><body><h1>" + title
                + "</h1><p>Toto okno můžete zavřít a vrátit se do hry Squares.</p></body></html>";
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static URI secureEndpoint(JsonNode json, String field) throws AuthenticationException {
        return validateEndpoint(URI.create(requiredText(json, field)));
    }

    private static URI optionalSecureEndpoint(JsonNode json, String field) throws AuthenticationException {
        String value = text(json, field);
        return isBlank(value) ? null : validateEndpoint(URI.create(value));
    }

    private static URI validateEndpoint(URI endpoint) throws AuthenticationException {
        if (!OidcConfiguration.isSecureOrLoopback(endpoint)) {
            throw new AuthenticationException("OIDC endpoint musí používat HTTPS (mimo lokální vývoj).");
        }
        return endpoint;
    }

    private static String requiredText(JsonNode json, String field) throws AuthenticationException {
        String value = text(json, field);
        if (isBlank(value)) {
            throw new AuthenticationException("OIDC odpověď neobsahuje pole " + field + ".");
        }
        return value;
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private String errorText(byte[] body) {
        try {
            JsonNode json = mapper.readTree(body);
            String description = text(json, "error_description");
            return isBlank(description) ? text(json, "error") : description;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String form(String[][] values) {
        StringBuilder result = new StringBuilder();
        for (String[] pair : values) {
            if (pair[1] == null) {
                continue;
            }
            if (result.length() > 0) {
                result.append('&');
            }
            result.append(urlEncode(pair[0])).append('=').append(urlEncode(pair[1]));
        }
        return result.toString();
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static boolean secureEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static URI withoutTrailingSlash(URI uri) {
        String value = uri.toString();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return URI.create(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class ProviderMetadata {
        private final URI authorizationEndpoint;
        private final URI tokenEndpoint;
        private final URI revocationEndpoint;

        private ProviderMetadata(URI authorizationEndpoint, URI tokenEndpoint, URI revocationEndpoint) {
            this.authorizationEndpoint = authorizationEndpoint;
            this.tokenEndpoint = tokenEndpoint;
            this.revocationEndpoint = revocationEndpoint;
        }
    }

    private static final class Callback {
        private final String code;
        private final String state;
        private final String error;

        private Callback(String code, String state, String error) {
            this.code = code;
            this.state = state;
            this.error = error;
        }

        private static Callback from(String rawQuery) {
            Map<String, String> values = new LinkedHashMap<>();
            if (rawQuery != null) {
                for (String pair : rawQuery.split("&")) {
                    int separator = pair.indexOf('=');
                    String key = urlDecode(separator < 0 ? pair : pair.substring(0, separator));
                    String value = urlDecode(separator < 0 ? "" : pair.substring(separator + 1));
                    if (!values.containsKey(key)) {
                        values.put(key, value);
                    }
                }
            }
            return new Callback(values.get("code"), values.get("state"), values.get("error"));
        }
    }
}
