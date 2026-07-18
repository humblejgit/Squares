package cz.codex.squares;

import java.util.List;

interface StatisticsStore {
    List<LocalProfileStatistics> findLocalLeaderboard() throws StorageException;
}
