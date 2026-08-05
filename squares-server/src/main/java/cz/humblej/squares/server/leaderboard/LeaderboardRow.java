package cz.humblej.squares.server.leaderboard;

import java.time.Instant;
import java.util.UUID;

import cz.humblej.squares.server.identity.PublicPlayerResponse;

record LeaderboardRow(
        long rank,
        UUID playerId,
        String handle,
        String displayName,
        long revision,
        Instant createdAt,
        long games,
        long wins,
        long draws,
        long losses,
        long totalScore) {
    LeaderboardEntryResponse response() {
        return new LeaderboardEntryResponse(
                rank,
                new PublicPlayerResponse(
                        playerId, handle, displayName, revision, createdAt),
                StatisticsResponse.of(games, wins, draws, losses, totalScore),
                null);
    }
}
