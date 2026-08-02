package cz.humblej.identity.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Clock;
import java.time.Instant;

public final class TokenSet {
    private final String accessToken;
    private final String refreshToken;
    private final String idToken;
    private final Instant accessTokenExpiresAt;

    @JsonCreator
    public TokenSet(@JsonProperty("accessToken") String accessToken,
                    @JsonProperty("refreshToken") String refreshToken,
                    @JsonProperty("idToken") String idToken,
                    @JsonProperty("accessTokenExpiresAt") Instant accessTokenExpiresAt) {
        if (isBlank(accessToken) || accessTokenExpiresAt == null) {
            throw new IllegalArgumentException("Access token and its expiration are required.");
        }
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.idToken = idToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public Instant getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public boolean expiresWithin(Clock clock, long seconds) {
        return !accessTokenExpiresAt.isAfter(clock.instant().plusSeconds(seconds));
    }

    public boolean canRefresh() {
        return !isBlank(refreshToken);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
