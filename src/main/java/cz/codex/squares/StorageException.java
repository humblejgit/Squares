package cz.codex.squares;

final class StorageException extends Exception {
    StorageException(String message) {
        super(message);
    }

    StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
