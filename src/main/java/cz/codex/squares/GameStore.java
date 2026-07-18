package cz.codex.squares;

interface GameStore {
    boolean save(GameResult result) throws StorageException;

    long countGames() throws StorageException;

    long countPendingOutboxEvents() throws StorageException;
}
