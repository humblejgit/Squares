package cz.humblej.squares.persistence;

import cz.humblej.squares.ui.Messages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public final class SqlitePlayerIdentityStore implements PlayerIdentityStore {
    private static final String INSTALLATION_ID_KEY = "installation_id";

    private final LocalDatabase database;

    public SqlitePlayerIdentityStore(LocalDatabase database) {
        this.database = database;
    }

    @Override
    public UUID getOrCreateInstallationId() throws StorageException {
        UUID candidate = UUID.randomUUID();
        String insert = "INSERT INTO app_settings(key, value) VALUES (?, ?) "
                + "ON CONFLICT(key) DO NOTHING";
        String select = "SELECT value FROM app_settings WHERE key=?";

        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insertStatement = connection.prepareStatement(insert);
                 PreparedStatement selectStatement = connection.prepareStatement(select)) {
                insertStatement.setString(1, INSTALLATION_ID_KEY);
                insertStatement.setString(2, candidate.toString());
                insertStatement.executeUpdate();
                selectStatement.setString(1, INSTALLATION_ID_KEY);
                try (ResultSet result = selectStatement.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("Installation ID was not persisted.");
                    }
                    UUID installationId = UUID.fromString(result.getString(1));
                    connection.commit();
                    return installationId;
                }
            } catch (SQLException | IllegalArgumentException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.INSTALLATION_ID_FAILED, exception);
        }
    }

    @Override
    public ProfileServerLink findLink(UUID localProfileId) throws StorageException {
        String sql = "SELECT local_profile_id, server_player_id, installation_id, linked_at "
                + "FROM profile_server_links WHERE local_profile_id=?";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, localProfileId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readLink(result) : null;
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.PROFILE_LINK_LOAD_FAILED, exception);
        }
    }

    @Override
    public ProfileServerLink link(UUID localProfileId, UUID playerId, UUID installationId)
            throws StorageException {
        String select = "SELECT local_profile_id, server_player_id, installation_id, linked_at "
                + "FROM profile_server_links WHERE local_profile_id=?";
        String insert = "INSERT INTO profile_server_links("
                + "local_profile_id, server_player_id, installation_id, linked_at) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                ProfileServerLink existing;
                try (PreparedStatement statement = connection.prepareStatement(select)) {
                    statement.setString(1, localProfileId.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        existing = result.next() ? readLink(result) : null;
                    }
                }

                if (existing != null) {
                    if (!existing.playerId().equals(playerId)) {
                        throw new StorageException(Messages.PROFILE_LINK_DIFFERENT_ACCOUNT);
                    }
                    connection.commit();
                    return existing;
                }

                Instant linkedAt = Instant.ofEpochMilli(Instant.now().toEpochMilli());
                try (PreparedStatement statement = connection.prepareStatement(insert)) {
                    statement.setString(1, localProfileId.toString());
                    statement.setString(2, playerId.toString());
                    statement.setString(3, installationId.toString());
                    statement.setLong(4, linkedAt.toEpochMilli());
                    statement.executeUpdate();
                }
                connection.commit();
                return new ProfileServerLink(localProfileId, playerId, installationId, linkedAt);
            } catch (SQLException | StorageException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (StorageException exception) {
            throw exception;
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.PROFILE_LINK_SAVE_FAILED, exception);
        }
    }

    @Override
    public void unlink(UUID localProfileId) throws StorageException {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM profile_server_links WHERE local_profile_id=?")) {
            statement.setString(1, localProfileId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException(Messages.PROFILE_UNLINK_FAILED, exception);
        }
    }

    private static ProfileServerLink readLink(ResultSet result) throws SQLException {
        return new ProfileServerLink(
                UUID.fromString(result.getString("local_profile_id")),
                UUID.fromString(result.getString("server_player_id")),
                UUID.fromString(result.getString("installation_id")),
                Instant.ofEpochMilli(result.getLong("linked_at")));
    }
}
