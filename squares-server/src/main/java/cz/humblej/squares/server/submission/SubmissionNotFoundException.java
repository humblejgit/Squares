package cz.humblej.squares.server.submission;

final class SubmissionNotFoundException extends RuntimeException {
    SubmissionNotFoundException() {
        super("game-submission-not-found");
    }
}
