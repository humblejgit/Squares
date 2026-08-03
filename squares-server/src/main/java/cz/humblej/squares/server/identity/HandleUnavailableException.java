package cz.humblej.squares.server.identity;

final class HandleUnavailableException extends RuntimeException {
    HandleUnavailableException() {
        super("handle-unavailable");
    }
}
