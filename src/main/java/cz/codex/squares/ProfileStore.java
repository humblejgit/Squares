package cz.codex.squares;

import java.util.List;
import java.util.UUID;

interface ProfileStore {
    List<PlayerProfile> findActive() throws StorageException;

    PlayerProfile findSelected() throws StorageException;

    PlayerProfile create(String displayName) throws StorageException;

    PlayerProfile rename(UUID id, String displayName) throws StorageException;

    void archive(UUID id) throws StorageException;

    void select(UUID id) throws StorageException;
}
