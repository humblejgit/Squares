package cz.humblej.squares.auth;

public final class OnlinePlayer {
    private final String playerId;
    private final String handle;
    private final String displayName;
    private final long revision;

    public OnlinePlayer(String playerId, String handle, String displayName, long revision) {
        this.playerId = playerId;
        this.handle = handle;
        this.displayName = displayName;
        this.revision = revision;
    }

    public String playerId() {
        return playerId;
    }

    public String handle() {
        return handle;
    }

    public String displayName() {
        return displayName;
    }

    public long revision() {
        return revision;
    }
}
