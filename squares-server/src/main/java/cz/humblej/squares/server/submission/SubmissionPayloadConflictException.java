package cz.humblej.squares.server.submission;

final class SubmissionPayloadConflictException extends RuntimeException {
    SubmissionPayloadConflictException() {
        super("submission-payload-conflict");
    }
}
