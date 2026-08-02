package cz.humblej.identity.client;

import java.net.URI;

public final class OidcConfiguration {
    private final URI issuer;
    private final URI apiBaseUri;
    private final String clientId;

    public OidcConfiguration(URI issuer, URI apiBaseUri, String clientId) {
        this.issuer = withoutTrailingSlash(issuer);
        this.apiBaseUri = withoutTrailingSlash(apiBaseUri);
        this.clientId = clientId;
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

    public URI discoveryUri() {
        return URI.create(issuer.toString() + "/.well-known/openid-configuration");
    }

    public static boolean isSecureOrLoopback(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
                || ("http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost())
                || "127.0.0.1".equals(uri.getHost())));
    }

    private static URI withoutTrailingSlash(URI value) {
        String text = value.toString();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return URI.create(text);
    }
}
