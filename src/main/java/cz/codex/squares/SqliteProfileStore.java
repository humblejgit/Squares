package cz.codex.squares;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class SqliteProfileStore implements ProfileStore {
    private static final String SELECTED_PROFILE_KEY = "selected_profile_id";

    private final LocalDatabase database;

    SqliteProfileStore(LocalDatabase database) {
        this.database = database;
    }

    @Override
    public List<PlayerProfile> findActive() throws StorageException {
        String sql = "SELECT id, display_name, created_at, archived FROM profiles "
                + "WHERE archived=0 ORDER BY normalized_name, created_at";
        List<PlayerProfile> profiles = new ArrayList<>();

        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                profiles.add(readProfile(result));
            }
            return profiles;
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.PROFILE_LIST_LOAD_FAILED, exception);
        }
    }

    @Override
    public PlayerProfile findSelected() throws StorageException {
        String sql = "SELECT p.id, p.display_name, p.created_at, p.archived "
                + "FROM profiles p JOIN app_settings s ON s.value=p.id "
                + "WHERE s.key=? AND p.archived=0";

        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SELECTED_PROFILE_KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readProfile(result) : null;
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.PROFILE_SELECTED_LOAD_FAILED, exception);
        }
    }

    @Override
    public PlayerProfile create(String displayName) throws StorageException {
        PlayerProfile profile = PlayerProfile.create(displayName);
        String sql = "INSERT INTO profiles(id, display_name, normalized_name, created_at, archived) "
                + "VALUES (?, ?, ?, ?, 0)";

        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.id().toString());
            statement.setString(2, profile.displayName());
            statement.setString(3, normalize(profile.displayName()));
            statement.setLong(4, profile.createdAt().toEpochMilli());
            statement.executeUpdate();
            return profile;
        } catch (SQLException exception) {
            throw profileWriteException(exception);
        }
    }

    @Override
    public PlayerProfile rename(UUID id, String displayName) throws StorageException {
        PlayerProfile existing = findById(id);
        PlayerProfile renamed = new PlayerProfile(existing.id(), displayName, existing.createdAt(), false);
        String sql = "UPDATE profiles SET display_name=?, normalized_name=? WHERE id=? AND archived=0";

        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, renamed.displayName());
            statement.setString(2, normalize(renamed.displayName()));
            statement.setString(3, id.toString());
            if (statement.executeUpdate() != 1) {
                throw new StorageException(Messages.PROFILE_NOT_FOUND);
            }
            return renamed;
        } catch (SQLException exception) {
            throw profileWriteException(exception);
        }
    }

    @Override
    public void archive(UUID id) throws StorageException {
        String updateProfile = "UPDATE profiles SET archived=1 WHERE id=? AND archived=0";
        String removeSelection = "DELETE FROM app_settings WHERE key=? AND value=?";

        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement profileStatement = connection.prepareStatement(updateProfile);
                 PreparedStatement selectionStatement = connection.prepareStatement(removeSelection)) {
                profileStatement.setString(1, id.toString());
                if (profileStatement.executeUpdate() != 1) {
                    throw new StorageException(Messages.PROFILE_NOT_FOUND);
                }
                selectionStatement.setString(1, SELECTED_PROFILE_KEY);
                selectionStatement.setString(2, id.toString());
                selectionStatement.executeUpdate();
                connection.commit();
            } catch (SQLException | StorageException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new StorageException(Messages.PROFILE_ARCHIVE_FAILED, exception);
        }
    }

    @Override
    public void select(UUID id) throws StorageException {
        PlayerProfile profile = findById(id);
        if (profile.archived()) {
            throw new StorageException(Messages.PROFILE_ARCHIVED_CANNOT_SELECT);
        }

        String sql = "INSERT INTO app_settings(key, value) VALUES (?, ?) "
                + "ON CONFLICT(key) DO UPDATE SET value=excluded.value";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SELECTED_PROFILE_KEY);
            statement.setString(2, id.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException(Messages.PROFILE_SELECTION_SAVE_FAILED, exception);
        }
    }

    private PlayerProfile findById(UUID id) throws StorageException {
        String sql = "SELECT id, display_name, created_at, archived FROM profiles WHERE id=?";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new StorageException(Messages.PROFILE_NOT_FOUND);
                }
                return readProfile(result);
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.PROFILE_LOAD_FAILED, exception);
        }
    }

    private static PlayerProfile readProfile(ResultSet result) throws SQLException {
        return new PlayerProfile(UUID.fromString(result.getString("id")),
                result.getString("display_name"),
                Instant.ofEpochMilli(result.getLong("created_at")),
                result.getInt("archived") != 0);
    }

    private static String normalize(String displayName) {
        return displayName.trim().toLowerCase(Locale.ROOT);
    }

    private static StorageException profileWriteException(SQLException exception) {
        String message = exception.getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("unique")) {
            return new StorageException(Messages.PROFILE_DUPLICATE_NAME, exception);
        }
        return new StorageException(Messages.PROFILE_SAVE_FAILED, exception);
    }
}
