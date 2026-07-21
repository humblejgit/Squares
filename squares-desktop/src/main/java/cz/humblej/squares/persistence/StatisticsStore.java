package cz.humblej.squares.persistence;

import cz.humblej.squares.model.LocalProfileStatistics;

import java.util.List;

public interface StatisticsStore {
    public List<LocalProfileStatistics> findLocalLeaderboard() throws StorageException;
}
