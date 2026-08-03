package cz.humblej.squares.auth;

import java.time.Instant;
import java.util.UUID;

public final class GameSubmissionStatus {
    private final UUID gameId;
    private final String submissionStatus;
    private final String verificationStatus;
    private final boolean ranked;
    private final Instant receivedAt;
    private final Instant updatedAt;

    public GameSubmissionStatus(UUID gameId, String submissionStatus, String verificationStatus,
                                boolean ranked, Instant receivedAt, Instant updatedAt) {
        this.gameId = gameId;
        this.submissionStatus = submissionStatus;
        this.verificationStatus = verificationStatus;
        this.ranked = ranked;
        this.receivedAt = receivedAt;
        this.updatedAt = updatedAt;
    }

    public UUID gameId() { return gameId; }
    public String submissionStatus() { return submissionStatus; }
    public String verificationStatus() { return verificationStatus; }
    public boolean ranked() { return ranked; }
    public Instant receivedAt() { return receivedAt; }
    public Instant updatedAt() { return updatedAt; }
}
