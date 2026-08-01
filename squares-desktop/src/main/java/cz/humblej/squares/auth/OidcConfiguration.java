package cz.humblej.squares.auth;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class OidcConfiguration {
    private final URI issuer;
    private final URI apiBaseUri;
    private final String clientId;
    private final Path tokenPath;

    public OidcConfiguration(URI issuer, URI apiBaseUri, String clientId, Path tokenPath) {
        this.issuer = withoutTrailingSlash(issuer);
        this.apiBaseUri = withoutTrailingSlash(apiBaseUri);
        this.clientId = clientId;
        this.tokenPath = tokenPath;
    }

    public static OidcConfiguration systemDefault() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path dataDirectory = localAppData == null || localAppData.trim().isEmpty()
                ? Paths.get(System.getProperty("user.home"), ".squares")
                : Paths.get(localAppData, "Squares");
        return new OidcConfiguration(
                URI.create(setting("squares.oidc.issuer", "SQUARES_DESKTOP_OIDC_ISSUER",
                        "http://localhost:9090/realms/squares")),
                URI.create(setting("squares.api.base-uri", "SQUARES_DESKTOP_API_BASE_URI",
                        "http://localhost:8080/api/v1")),
                setting("squares.oidc.client-id", "SQUARES_DESKTOP_OIDC_CLIENT_ID", "squares-desktop"),
                dataDirectory.resolve("oidc-session.dat"));
    }

    public URI issuer() {
        return issuer;
    }

    public URI apiBaseUri() {
        return apiBaseUri;
    }

    public String clientId() {
        return clientId;
    }

    public Path tokenPath() {
        return tokenPath;
    }

    URI discoveryUri() {
        return URI.create(issuer.toString() + "/.well-known/openid-configuration");
    }

    static boolean isSecureOrLoopback(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
                || ("http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost())
                || "127.0.0.1".equals(uri.getHost())));
    }

    private static String setting(String property, String environment, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(environment);
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static URI withoutTrailingSlash(URI value) {
        String text = value.toString();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return URI.create(text);
    }
}
