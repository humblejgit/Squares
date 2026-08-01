package cz.humblej.squares.server.identity;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

public record MeResponse(
        String accountStatus,
        boolean onboardingRequired,
        @JsonInclude(JsonInclude.Include.NON_NULL) PublicPlayerResponse player,
        Instant createdAt) {
}
