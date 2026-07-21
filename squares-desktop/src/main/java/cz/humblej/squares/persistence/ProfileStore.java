package cz.humblej.squares.persistence;

import cz.humblej.squares.model.PlayerProfile;

import java.util.List;
import java.util.UUID;

public interface ProfileStore {
    public List<PlayerProfile> findActive() throws StorageException;

    public PlayerProfile findSelected() throws StorageException;

    public PlayerProfile create(String displayName) throws StorageException;

    public PlayerProfile rename(UUID id, String displayName) throws StorageException;

    public void archive(UUID id) throws StorageException;

    public void select(UUID id) throws StorageException;
}
