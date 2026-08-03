package cz.humblej.squares.persistence;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import cz.humblej.squares.auth.GameSubmissionStatus;

public interface GameSyncStore {
    void recoverInterruptedSync() throws StorageException;

    OutboxEvent claimNext(UUID playerId, Instant now, boolean includeDeferred)
            throws StorageException;

    void markSent(UUID eventId, GameSubmissionStatus status, Instant syncedAt)
            throws StorageException;

    void markRetry(UUID eventId, String error, Instant nextAttemptAt)
            throws StorageException;

    void markDead(UUID eventId, String error) throws StorageException;

    List<UUID> findPendingPeerSubmissions(UUID playerId, int limit)
            throws StorageException;

    SyncSummary summary(UUID playerId) throws StorageException;
}
