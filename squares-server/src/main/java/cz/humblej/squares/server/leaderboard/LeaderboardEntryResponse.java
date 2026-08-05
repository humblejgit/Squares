package cz.humblej.squares.server.leaderboard;

import com.fasterxml.jackson.annotation.JsonInclude;

import cz.humblej.squares.server.identity.PublicPlayerResponse;

public record LeaderboardEntryResponse(
        long rank,
        PublicPlayerResponse player,
        StatisticsResponse statistics,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer rating) {
}
