package cz.humblej.squares.server.identity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ProfileRepository {
    private final JdbcTemplate jdbc;

    ProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<PlayerRecord> findPlayer(UUID playerId) {
        List<PlayerRecord> players = jdbc.query("""
                SELECT player_id, handle, display_name, revision, created_at
                FROM players
                WHERE player_id = ?
                """, (result, row) -> new PlayerRecord(
                        result.getObject("player_id", UUID.class),
                        result.getString("handle"),
                        result.getString("display_name"),
                        result.getLong("revision"),
                        result.getTimestamp("created_at").toInstant()),
                playerId);
        return players.stream().findFirst();
    }

    PlayerRecord insertPlayer(
            UUID playerId, String handle, String normalizedHandle,
            String displayName, Instant now) {
        return jdbc.queryForObject("""
                INSERT INTO players (
                    player_id, handle, normalized_handle, display_name,
                    revision, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, 1, ?, ?)
                RETURNING player_id, handle, display_name, revision, created_at
                """, (result, row) -> new PlayerRecord(
                        result.getObject("player_id", UUID.class),
                        result.getString("handle"),
                        result.getString("display_name"),
                        result.getLong("revision"),
                        result.getTimestamp("created_at").toInstant()),
                playerId, handle, normalizedHandle, displayName,
                Timestamp.from(now), Timestamp.from(now));
    }

    PlayerRecord updatePlayer(
            UUID playerId, String handle, String normalizedHandle,
            String displayName, Instant now) {
        return jdbc.queryForObject("""
                UPDATE players
                SET handle = ?, normalized_handle = ?, display_name = ?,
                    revision = revision + 1, updated_at = ?
                WHERE player_id = ?
                RETURNING player_id, handle, display_name, revision, created_at
                """, (result, row) -> new PlayerRecord(
                        result.getObject("player_id", UUID.class),
                        result.getString("handle"),
                        result.getString("display_name"),
                        result.getLong("revision"),
                        result.getTimestamp("created_at").toInstant()),
                handle, normalizedHandle, displayName, Timestamp.from(now), playerId);
    }
}
