package cz.humblej.squares.persistence;

import cz.humblej.squares.model.LocalProfileStatistics;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.ui.Messages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SqliteStatisticsStore implements StatisticsStore {
    private final LocalDatabase database;

    public SqliteStatisticsStore(LocalDatabase database) {
        this.database = database;
    }

    @Override
    public List<LocalProfileStatistics> findLocalLeaderboard() throws StorageException {
        String sql = "SELECT p.id, p.display_name, p.created_at, p.archived, "
                + "COUNT(gp.game_id) AS games, "
                + "SUM(CASE WHEN gp.outcome='WIN' THEN 1 ELSE 0 END) AS wins, "
                + "SUM(CASE WHEN gp.outcome='DRAW' THEN 1 ELSE 0 END) AS draws, "
                + "SUM(CASE WHEN gp.outcome='LOSS' THEN 1 ELSE 0 END) AS losses, "
                + "COALESCE(SUM(gp.score), 0) AS total_score "
                + "FROM profiles p "
                + "LEFT JOIN game_players gp ON gp.profile_id=p.id AND gp.player_type='PROFILE' "
                + "GROUP BY p.id, p.display_name, p.created_at, p.archived, p.normalized_name "
                + "ORDER BY total_score DESC, wins DESC, games DESC, p.normalized_name ASC";
        List<LocalProfileStatistics> leaderboard = new ArrayList<>();

        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                PlayerProfile profile = new PlayerProfile(
                        UUID.fromString(result.getString("id")),
                        result.getString("display_name"),
                        Instant.ofEpochMilli(result.getLong("created_at")),
                        result.getInt("archived") != 0);
                leaderboard.add(new LocalProfileStatistics(profile,
                        result.getLong("games"),
                        result.getLong("wins"),
                        result.getLong("draws"),
                        result.getLong("losses"),
                        result.getLong("total_score")));
            }
            return leaderboard;
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException(Messages.STATISTICS_LOAD_FAILED, exception);
        }
    }
}
