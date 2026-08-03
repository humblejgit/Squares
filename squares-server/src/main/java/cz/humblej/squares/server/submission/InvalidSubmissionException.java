package cz.humblej.squares.server.submission;

final class InvalidSubmissionException extends RuntimeException {
    private final String detailKey;

    InvalidSubmissionException(String detailKey) {
        super(detailKey);
        this.detailKey = detailKey;
    }

    String detailKey() {
        return detailKey;
    }
}
