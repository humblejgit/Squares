package cz.humblej.squares.server.submission;

import java.time.Instant;
import java.util.UUID;

public record GameSubmissionStatusResponse(
        UUID gameId,
        String submissionStatus,
        String verificationStatus,
        boolean ranked,
        Instant receivedAt,
        Instant updatedAt) {
}
