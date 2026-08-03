package cz.humblej.squares.server.submission;

final class InstallationNotRegisteredException extends RuntimeException {
    InstallationNotRegisteredException() {
        super("installation-not-registered");
    }
}
