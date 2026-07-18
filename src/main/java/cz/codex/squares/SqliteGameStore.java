package cz.codex.squares;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class SqliteGameStore implements GameStore {
    private final LocalDatabase database;

    SqliteGameStore(LocalDatabase database) {
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
        return count("SELECT COUNT(*) FROM outbox WHERE state='PENDING'");
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
