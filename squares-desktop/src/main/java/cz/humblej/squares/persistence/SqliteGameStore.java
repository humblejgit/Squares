package cz.humblej.squares.persistence;

import cz.humblej.squares.codec.GameResultCodec;
import cz.humblej.squares.auth.GameSubmissionStatus;
import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerResult;
import cz.humblej.squares.ui.Messages;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class SqliteGameStore implements GameStore, GameSyncStore {
    private final LocalDatabase database;

    public SqliteGameStore(LocalDatabase database) {
        this.database = database;
    }

    @Override
    public boolean save(GameResult result) throws StorageException {
        String insertGame = "INSERT OR IGNORE INTO games(id, started_at, finished_at, mode, finish_reason, "
                + "rows, columns, thinking_time_limit_seconds, total_seconds, random_initial_edges, cpu_difficulty) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                if (!insertGame(connection, insertGame, result)) {
                    connection.rollback();
                    return false;
                }
                insertPlayer(connection, result.gameId().toString(), result.redPlayer());
                insertPlayer(connection, result.gameId().toString(), result.bluePlayer());
                insertOutboxEvent(connection, result);
                connection.commit();
                return true;
            } catch (SQLException | IOException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException | IOException exception) {
            throw new StorageException(Messages.GAME_RESULT_SAVE_FAILED, exception);
        }
    }

    @Override
    public long countGames() throws StorageException {
        return count("SELECT COUNT(*) FROM games");
    }

    @Override
    public long countPendingOutboxEvents() throws StorageException {
        return count("SELECT COUNT(*) FROM outbox WHERE state IN ('PENDING','SENDING','RETRY')");
    }

    @Override
    public void recoverInterruptedSync() throws StorageException {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE outbox SET state='RETRY', next_attempt_at=NULL, "
                             + "last_error='Předchozí synchronizace byla přerušena.' "
                             + "WHERE state='SENDING'")) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException(Messages.SYNC_STATE_SAVE_FAILED, exception);
        }
    }

    @Override
    public OutboxEvent claimNext(UUID playerId, Instant now, boolean includeDeferred)
            throws StorageException {
        String select = "SELECT o.event_id, o.payload, gp.seat, o.attempts "
                + "FROM outbox o "
                + "JOIN game_players gp ON gp.game_id=o.aggregate_id "
                + "JOIN profile_server_links l ON l.local_profile_id=gp.profile_id "
                + "WHERE l.server_player_id=? AND o.state IN ('PENDING','RETRY') "
                + (includeDeferred ? "" : "AND COALESCE(o.next_attempt_at,0)<=? ")
                + "ORDER BY o.created_at LIMIT 1";

        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                OutboxEvent event = null;
                try (PreparedStatement statement = connection.prepareStatement(select)) {
                    statement.setString(1, playerId.toString());
                    if (!includeDeferred) {
                        statement.setLong(2, now.toEpochMilli());
                    }
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            event = new OutboxEvent(
                                    UUID.fromString(result.getString("event_id")),
                                    result.getString("payload"),
                                    PlayerResult.Seat.valueOf(result.getString("seat")),
                                    result.getInt("attempts") + 1);
                        }
                    }
                }
                if (event != null) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE outbox SET state='SENDING', attempts=attempts+1, "
                                    + "last_error=NULL WHERE event_id=? "
                                    + "AND state IN ('PENDING','RETRY')")) {
                        statement.setString(1, event.eventId().toString());
                        if (statement.executeUpdate() != 1) {
                            event = null;
                        }
                    }
                }
                connection.commit();
                return event;
            } catch (SQLException | IllegalArgumentException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.SYNC_STATE_LOAD_FAILED, exception);
        }
    }

    @Override
    public void markSent(UUID eventId, GameSubmissionStatus status, Instant syncedAt)
            throws StorageException {
        String sql = "UPDATE outbox SET state='SENT', last_error=NULL, next_attempt_at=NULL, "
                + "submission_status=?, verification_status=?, server_updated_at=?, synced_at=? "
                + "WHERE event_id=?";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.submissionStatus());
            statement.setString(2, status.verificationStatus());
            statement.setLong(3, status.updatedAt().toEpochMilli());
            statement.setLong(4, syncedAt.toEpochMilli());
            statement.setString(5, eventId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException(Messages.SYNC_STATE_SAVE_FAILED, exception);
        }
    }

    @Override
    public void markRetry(UUID eventId, String error, Instant nextAttemptAt)
            throws StorageException {
        updateFailure(eventId, "RETRY", error, nextAttemptAt);
    }

    @Override
    public void markDead(UUID eventId, String error) throws StorageException {
        updateFailure(eventId, "DEAD", error, null);
    }

    @Override
    public List<UUID> findPendingPeerSubmissions(UUID playerId, int limit)
            throws StorageException {
        String sql = "SELECT DISTINCT o.event_id FROM outbox o "
                + "JOIN game_players gp ON gp.game_id=o.aggregate_id "
                + "JOIN profile_server_links l ON l.local_profile_id=gp.profile_id "
                + "WHERE l.server_player_id=? AND o.state='SENT' "
                + "AND o.submission_status='PENDING_PEER' "
                + "ORDER BY o.event_id LIMIT ?";
        List<UUID> gameIds = new ArrayList<UUID>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    gameIds.add(UUID.fromString(result.getString(1)));
                }
            }
            return gameIds;
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.SYNC_STATE_LOAD_FAILED, exception);
        }
    }

    @Override
    public SyncSummary summary(UUID playerId) throws StorageException {
        String sql = "SELECT o.state, COUNT(DISTINCT o.event_id) AS count "
                + "FROM outbox o "
                + "JOIN game_players gp ON gp.game_id=o.aggregate_id "
                + "JOIN profile_server_links l ON l.local_profile_id=gp.profile_id "
                + "WHERE l.server_player_id=? GROUP BY o.state";
        long pending = 0;
        long sending = 0;
        long sent = 0;
        long failed = 0;
        long pendingPeer = 0;
        long matched = 0;
        long conflicted = 0;
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String state = result.getString("state");
                    long value = result.getLong("count");
                    if ("PENDING".equals(state) || "RETRY".equals(state)) {
                        pending += value;
                    } else if ("SENDING".equals(state)) {
                        sending += value;
                    } else if ("SENT".equals(state)) {
                        sent += value;
                    } else if ("DEAD".equals(state)) {
                        failed += value;
                    }
                }
            }
            try (PreparedStatement remote = connection.prepareStatement(
                    "SELECT o.submission_status, COUNT(DISTINCT o.event_id) AS count "
                            + "FROM outbox o "
                            + "JOIN game_players gp ON gp.game_id=o.aggregate_id "
                            + "JOIN profile_server_links l ON l.local_profile_id=gp.profile_id "
                            + "WHERE l.server_player_id=? AND o.state='SENT' "
                            + "GROUP BY o.submission_status")) {
                remote.setString(1, playerId.toString());
                try (ResultSet result = remote.executeQuery()) {
                    while (result.next()) {
                        String status = result.getString("submission_status");
                        long value = result.getLong("count");
                        if ("PENDING_PEER".equals(status)) {
                            pendingPeer += value;
                        } else if ("MATCHED".equals(status)) {
                            matched += value;
                        } else if ("CONFLICTED".equals(status)) {
                            conflicted += value;
                        }
                    }
                }
            }
            return new SyncSummary(pending, sending, sent, failed,
                    pendingPeer, matched, conflicted);
        } catch (SQLException exception) {
            throw new StorageException(Messages.SYNC_STATE_LOAD_FAILED, exception);
        }
    }

    private void updateFailure(UUID eventId, String state, String error, Instant nextAttemptAt)
            throws StorageException {
        String sql = "UPDATE outbox SET state=?, last_error=?, next_attempt_at=? WHERE event_id=?";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state);
            statement.setString(2, truncate(error, 1000));
            if (nextAttemptAt == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, nextAttemptAt.toEpochMilli());
            }
            statement.setString(4, eventId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException(Messages.SYNC_STATE_SAVE_FAILED, exception);
        }
    }

    private static String truncate(String value, int length) {
        if (value == null) {
            return null;
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    private static boolean insertGame(Connection connection, String sql, GameResult result) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, result.gameId().toString());
            statement.setLong(2, result.startedAt().toEpochMilli());
            statement.setLong(3, result.finishedAt().toEpochMilli());
            statement.setString(4, result.mode().name());
            statement.setString(5, result.finishReason().name());
            statement.setInt(6, result.rows());
            statement.setInt(7, result.columns());
            statement.setInt(8, result.thinkingTimeLimitSeconds());
            statement.setInt(9, result.totalSeconds());
            statement.setInt(10, result.randomInitialEdges() ? 1 : 0);
            if (result.cpuDifficulty() == null) {
                statement.setNull(11, java.sql.Types.VARCHAR);
            } else {
                statement.setString(11, result.cpuDifficulty().name());
            }
            return statement.executeUpdate() == 1;
        }
    }

    private static void insertPlayer(Connection connection, String gameId, PlayerResult player) throws SQLException {
        String sql = "INSERT INTO game_players(game_id, seat, player_type, profile_id, display_name, score, "
                + "thinking_seconds, outcome) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gameId);
            statement.setString(2, player.seat().name());
            statement.setString(3, player.playerType().name());
            if (player.hasProfile()) {
                statement.setString(4, player.profile().id().toString());
            } else {
                statement.setNull(4, java.sql.Types.VARCHAR);
            }
            statement.setString(5, player.displayName());
            statement.setInt(6, player.score());
            statement.setInt(7, player.thinkingSeconds());
            statement.setString(8, player.outcome().name());
            statement.executeUpdate();
        }
    }

    private static void insertOutboxEvent(Connection connection, GameResult result) throws SQLException, IOException {
        String sql = "INSERT INTO outbox(event_id, aggregate_type, aggregate_id, event_type, payload, created_at) "
                + "VALUES (?, 'GAME', ?, 'GAME_RESULT_RECORDED', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String id = result.gameId().toString();
            statement.setString(1, id);
            statement.setString(2, id);
            statement.setString(3, GameResultCodec.encode(result));
            statement.setLong(4, result.finishedAt().toEpochMilli());
            statement.executeUpdate();
        }
    }

    private long count(String sql) throws StorageException {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : 0L;
        } catch (SQLException exception) {
            throw new StorageException(Messages.DATABASE_READ_FAILED, exception);
        }
    }
}
