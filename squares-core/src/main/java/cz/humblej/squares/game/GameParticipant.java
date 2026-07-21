package cz.humblej.squares.game;

import cz.humblej.squares.model.PlayerProfile;

import java.util.Objects;

/** Platform-neutral identity of one participant in a game result. */
public final class GameParticipant {
    public enum Type {
        PROFILE,
        GUEST,
        COMPUTER
    }

    private final Type type;
    private final PlayerProfile profile;
    private final String displayName;

    public static GameParticipant profile(PlayerProfile profile) {
        PlayerProfile required = Objects.requireNonNull(profile, "profile");
        return new GameParticipant(Type.PROFILE, required, required.displayName());
    }

    public static GameParticipant guest(String displayName) {
        return new GameParticipant(Type.GUEST, null, displayName);
    }

    public static GameParticipant computer(String displayName) {
        return new GameParticipant(Type.COMPUTER, null, displayName);
    }

    private GameParticipant(Type type, PlayerProfile profile, String displayName) {
        this.type = Objects.requireNonNull(type, "type");
        this.profile = profile;
        this.displayName = requireDisplayName(displayName);
    }

    Type type() {
        return type;
    }

    PlayerProfile profile() {
        return profile;
    }

    String displayName() {
        return displayName;
    }

    private static String requireDisplayName(String displayName) {
        String normalized = Objects.requireNonNull(displayName, "displayName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Display name must not be empty.");
        }
        return normalized;
    }
}
