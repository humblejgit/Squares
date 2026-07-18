package cz.codex.squares;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class LocalDatabase {
    private static final int SCHEMA_VERSION = 1;

    private final Path databasePath;

    LocalDatabase(Path databasePath) {
        this.databasePath = databasePath.toAbsolutePath().normalize();
    }

    static LocalDatabase applicationDatabase() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path directory;

        if (localAppData != null && !localAppData.trim().isEmpty()) {
            directory = Paths.get(localAppData, "Squares");
        } else {
            directory = Paths.get(System.getProperty("user.home"), ".squares");
        }

        return new LocalDatabase(directory.resolve("squares.db"));
    }

    Path path() {
        return databasePath;
    }

    void initialize() throws StorageException {
        createParentDirectory();
        loadDriver();

        try (Connection connection = open()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
            }

            int version = schemaVersion(connection);

            if (version > SCHEMA_VERSION) {
                throw new StorageException(Messages.DATABASE_NEWER_SCHEMA);
            }

            if (version == 0) {
                createSchemaVersionOne(connection);
            }
        } catch (SQLException exception) {
            throw new StorageException(Messages.DATABASE_INITIALIZATION_FAILED, exception);
        }
    }

    Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
        }

        return connection;
    }

    private void createParentDirectory() throws StorageException {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            throw new StorageException(Messages.DATABASE_DIRECTORY_CREATE_FAILED, exception);
        }
    }

    private static void loadDriver() throws StorageException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new StorageException(Messages.DATABASE_SQLITE_DRIVER_MISSING, exception);
        }
    }

    private static int schemaVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static void createSchemaVersionOne(Connection connection) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE profiles ("
                    + "id TEXT PRIMARY KEY, "
                    + "display_name TEXT NOT NULL, "
                    + "normalized_name TEXT NOT NULL UNIQUE, "
                    + "created_at INTEGER NOT NULL, "
                    + "archived INTEGER NOT NULL DEFAULT 0 CHECK (archived IN (0, 1)))");
            statement.execute("CREATE TABLE app_settings ("
                    + "key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            statement.execute("CREATE TABLE games ("
                    + "id TEXT PRIMARY KEY, started_at INTEGER NOT NULL, finished_at INTEGER NOT NULL, "
                    + "mode TEXT NOT NULL, finish_reason TEXT NOT NULL, rows INTEGER NOT NULL, columns INTEGER NOT NULL, "
                    + "thinking_time_limit_seconds INTEGER NOT NULL, total_seconds INTEGER NOT NULL, "
                    + "random_initial_edges INTEGER NOT NULL, cpu_difficulty TEXT)");
            statement.execute("CREATE TABLE game_players ("
                    + "game_id TEXT NOT NULL, seat TEXT NOT NULL, player_type TEXT NOT NULL, profile_id TEXT, "
                    + "display_name TEXT NOT NULL, score INTEGER NOT NULL, thinking_seconds INTEGER NOT NULL, "
                    + "outcome TEXT NOT NULL, PRIMARY KEY (game_id, seat), "
                    + "FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE)");
            statement.execute("CREATE INDEX game_players_profile_id_idx ON game_players(profile_id)");
            statement.execute("CREATE TABLE outbox ("
                    + "event_id TEXT PRIMARY KEY, aggregate_type TEXT NOT NULL, aggregate_id TEXT NOT NULL UNIQUE, "
                    + "event_type TEXT NOT NULL, payload TEXT NOT NULL, state TEXT NOT NULL DEFAULT 'PENDING', "
                    + "attempts INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, last_error TEXT)");
            statement.execute("CREATE INDEX outbox_state_idx ON outbox(state, created_at)");
            statement.execute("PRAGMA user_version=" + SCHEMA_VERSION);
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }
}
