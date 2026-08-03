package cz.humblej.squares.server.submission;

import java.time.Instant;
import java.util.UUID;

record SubmissionRecord(
        UUID gameId,
        UUID playerId,
        byte[] payloadHash,
        String status,
        Instant receivedAt,
        Instant updatedAt,
        String verificationStatus,
        boolean ranked) {

    GameSubmissionStatusResponse response() {
        return new GameSubmissionStatusResponse(
                gameId, status, verificationStatus, ranked, receivedAt, updatedAt);
    }
}
