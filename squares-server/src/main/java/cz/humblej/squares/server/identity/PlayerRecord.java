package cz.humblej.squares.server.identity;

import java.time.Instant;
import java.util.UUID;

record PlayerRecord(
        UUID playerId,
        String handle,
        String displayName,
        long revision,
        Instant createdAt) {
}
