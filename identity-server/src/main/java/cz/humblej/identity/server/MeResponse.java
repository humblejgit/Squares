package cz.humblej.identity.server;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
        String accountStatus,
        UUID playerId,
        Instant createdAt) {
}
