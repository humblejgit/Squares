package cz.humblej.squares.persistence;

import cz.humblej.squares.model.GameResult;

public interface GameStore {
    public boolean save(GameResult result) throws StorageException;

    public long countGames() throws StorageException;

    public long countPendingOutboxEvents() throws StorageException;
}
