package cz.humblej.squares.server.identity;

final class HandleUnavailableException extends RuntimeException {
    HandleUnavailableException() {
        super("The requested handle is already in use.");
    }
}
