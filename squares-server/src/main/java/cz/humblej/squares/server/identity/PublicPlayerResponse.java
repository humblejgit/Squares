package cz.humblej.squares.server.identity;

import java.time.Instant;
import java.util.UUID;

public record PublicPlayerResponse(
        UUID playerId,
        String handle,
        String displayName,
        long revision,
        Instant createdAt) {
    static PublicPlayerResponse from(PlayerRecord player) {
        return new PublicPlayerResponse(
                player.playerId(),
                player.handle(),
                player.displayName(),
                player.revision(),
                player.createdAt());
    }
}
