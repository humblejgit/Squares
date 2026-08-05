package cz.humblej.squares.server.leaderboard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class LeaderboardRepository {
    private static final String COLUMNS = """
            rank, player_id, handle, display_name, revision, created_at,
            games, wins, draws, losses, total_score
            """;

    private final JdbcTemplate jdbc;

    LeaderboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<LeaderboardRow> findCasualPage(long afterRank, int limit) {
        return jdbc.query("""
                SELECT %s
                FROM casual_leaderboard
                WHERE rank > ?
                ORDER BY rank
                LIMIT ?
                """.formatted(COLUMNS), this::row, afterRank, limit);
    }

    Optional<LeaderboardRow> findCasualPlayer(UUID playerId) {
        List<LeaderboardRow> rows = jdbc.query("""
                SELECT %s
                FROM casual_leaderboard
                WHERE player_id = ?
                """.formatted(COLUMNS), this::row, playerId);
        return rows.stream().findFirst();
    }

    private LeaderboardRow row(java.sql.ResultSet result, int row) throws java.sql.SQLException {
        return new LeaderboardRow(
                result.getLong("rank"),
                result.getObject("player_id", UUID.class),
                result.getString("handle"),
                result.getString("display_name"),
                result.getLong("revision"),
                result.getTimestamp("created_at").toInstant(),
                result.getLong("games"),
                result.getLong("wins"),
                result.getLong("draws"),
                result.getLong("losses"),
                result.getLong("total_score"));
    }
}
