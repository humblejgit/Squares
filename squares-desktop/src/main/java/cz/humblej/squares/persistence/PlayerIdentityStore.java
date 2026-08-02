package cz.humblej.squares.persistence;

import java.util.UUID;

public interface PlayerIdentityStore {
    UUID getOrCreateInstallationId() throws StorageException;

    ProfileServerLink findLink(UUID localProfileId) throws StorageException;

    ProfileServerLink link(UUID localProfileId, UUID playerId, UUID installationId)
            throws StorageException;

    void unlink(UUID localProfileId) throws StorageException;
}
