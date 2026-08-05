package cz.humblej.squares.server.leaderboard;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record LeaderboardPageResponse(
        String board,
        String rankingMetric,
        List<LeaderboardEntryResponse> entries,
        @JsonInclude(JsonInclude.Include.NON_NULL) String nextCursor,
        Instant generatedAt) {
}
