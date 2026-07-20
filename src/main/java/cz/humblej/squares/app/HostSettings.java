package cz.humblej.squares.app;

final class HostSettings {
    private final GameOptions gameOptions;
    private final String hostAddress;
    private final int port;

    HostSettings(GameOptions gameOptions, String hostAddress, int port) {
        this.gameOptions = gameOptions;
        this.hostAddress = hostAddress;
        this.port = port;
    }

    GameOptions gameOptions() {
        return gameOptions;
    }

    String hostAddress() {
        return hostAddress;
    }

    int port() {
        return port;
    }

    boolean networkChanged(String currentHostAddress, int currentPort) {
        return !hostAddress.equals(currentHostAddress) || port != currentPort;
    }
}
