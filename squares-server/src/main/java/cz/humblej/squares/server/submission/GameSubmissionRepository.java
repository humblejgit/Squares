package cz.humblej.squares.server.submission;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class GameSubmissionRepository {
    private final JdbcTemplate jdbc;

    GameSubmissionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void lockGame(UUID gameId) {
        long lockKey = gameId.getMostSignificantBits() ^ gameId.getLeastSignificantBits();
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(?)", Object.class, lockKey);
    }

    boolean installationExists(UUID accountId, UUID installationId) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM installations
                    WHERE account_id = ? AND installation_id = ? AND revoked_at IS NULL
                )
                """, Boolean.class, accountId, installationId);
        return Boolean.TRUE.equals(exists);
    }

    Optional<SubmissionRecord> find(UUID gameId, UUID playerId) {
        List<SubmissionRecord> rows = jdbc.query("""
                SELECT s.game_id, s.submitted_by_player_id, s.payload_hash, s.status,
                       s.received_at, s.updated_at, g.verification_status, g.ranked
                FROM game_submissions s
                JOIN games g ON g.game_id = s.game_id
                WHERE s.game_id = ? AND s.submitted_by_player_id = ?
                """, (result, row) -> new SubmissionRecord(
                        result.getObject("game_id", UUID.class),
                        result.getObject("submitted_by_player_id", UUID.class),
                        result.getBytes("payload_hash"),
                        result.getString("status"),
                        result.getTimestamp("received_at").toInstant(),
                        result.getTimestamp("updated_at").toInstant(),
                        result.getString("verification_status"),
                        result.getBoolean("ranked")),
                gameId, playerId);
        return rows.stream().findFirst();
    }

    Optional<byte[]> findCanonicalHash(UUID gameId) {
        List<byte[]> rows = jdbc.query(
                "SELECT canonical_payload_hash FROM games WHERE game_id = ?",
                (result, row) -> result.getBytes(1), gameId);
        return rows.stream().findFirst().map(value -> Arrays.copyOf(value, value.length));
    }

    void insertGame(UUID gameId, String canonicalPayload, byte[] canonicalHash,
                    String verificationStatus, Instant startedAt, Instant finishedAt,
                    Instant now) {
        jdbc.update("""
                INSERT INTO games (
                    game_id, canonical_payload, canonical_payload_hash,
                    verification_status, ranked, started_at, finished_at,
                    created_at, updated_at
                ) VALUES (?, CAST(? AS jsonb), ?, ?, false, ?, ?, ?, ?)
                """, gameId, canonicalPayload, canonicalHash, verificationStatus,
                Timestamp.from(startedAt), Timestamp.from(finishedAt),
                Timestamp.from(now), Timestamp.from(now));
    }

    void insertGamePlayer(UUID gameId, SubmittedPlayerRequest player, Instant now) {
        jdbc.update("""
                INSERT INTO game_players (
                    game_id, seat, player_id, player_type, display_name_snapshot,
                    score, thinking_seconds, outcome
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (game_id, seat) DO UPDATE
                SET player_id = COALESCE(game_players.player_id, EXCLUDED.player_id)
                """, gameId, player.seat().name(), player.playerId(), player.playerType().name(),
                player.displayNameSnapshot().trim(), player.score(),
                player.thinkingSeconds(), player.outcome().name());
    }

    void insertSubmission(UUID gameId, UUID accountId, UUID playerId,
                          UUID installationId, String payload, byte[] payloadHash,
                          String status, Instant now) {
        jdbc.update("""
                INSERT INTO game_submissions (
                    submission_id, game_id, submitted_by_account_id,
                    submitted_by_player_id, installation_id, payload,
                    payload_hash, status, received_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
                """, UUID.randomUUID(), gameId, accountId, playerId, installationId,
                payload, payloadHash, status, Timestamp.from(now), Timestamp.from(now));
    }

    int countOtherSubmissions(UUID gameId, UUID playerId) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM game_submissions
                WHERE game_id = ? AND submitted_by_player_id <> ?
                """, Integer.class, gameId, playerId);
        return count == null ? 0 : count;
    }

    Optional<String> findOtherSubmittedSeat(UUID gameId, UUID playerId) {
        List<String> seats = jdbc.query("""
                SELECT payload ->> 'submittedBySeat'
                FROM game_submissions
                WHERE game_id = ? AND submitted_by_player_id <> ?
                ORDER BY received_at
                """, (result, row) -> result.getString(1), gameId, playerId);
        return seats.stream().findFirst();
    }

    void updateGameAndSubmissions(UUID gameId, String submissionStatus,
                                  String verificationStatus, Instant now) {
        jdbc.update("""
                UPDATE games SET verification_status = ?, updated_at = ?
                WHERE game_id = ?
                """, verificationStatus, Timestamp.from(now), gameId);
        jdbc.update("""
                UPDATE game_submissions SET status = ?, updated_at = ?
                WHERE game_id = ?
                """, submissionStatus, Timestamp.from(now), gameId);
    }
}
