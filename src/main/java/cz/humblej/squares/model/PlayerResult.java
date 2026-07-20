package cz.humblej.squares.model;

import java.util.Objects;

/** Snapshot of one participant at the end of a game. */
public final class PlayerResult {
    public enum Seat {
        RED,
        BLUE
    }

    public enum PlayerType {
        PROFILE,
        GUEST,
        CPU
    }

    public enum Outcome {
        WIN,
        LOSS,
        DRAW
    }

    private final Seat seat;
    private final PlayerType playerType;
    private final PlayerProfile profile;
    private final String displayName;
    private final int score;
    private final int thinkingSeconds;
    private final Outcome outcome;

    public static PlayerResult forProfile(Seat seat, PlayerProfile profile, int score,
                                   int thinkingSeconds, Outcome outcome) {
        PlayerProfile requiredProfile = Objects.requireNonNull(profile, "profile");
        return new PlayerResult(seat, PlayerType.PROFILE, requiredProfile,
                requiredProfile.displayName(), score, thinkingSeconds, outcome);
    }

    public static PlayerResult guest(Seat seat, String displayName, int score,
                              int thinkingSeconds, Outcome outcome) {
        return new PlayerResult(seat, PlayerType.GUEST, null,
                displayName, score, thinkingSeconds, outcome);
    }

    public static PlayerResult computer(Seat seat, String displayName, int score,
                                 int thinkingSeconds, Outcome outcome) {
        return new PlayerResult(seat, PlayerType.CPU, null,
                displayName, score, thinkingSeconds, outcome);
    }

    private PlayerResult(Seat seat, PlayerType playerType, PlayerProfile profile,
                         String displayName, int score, int thinkingSeconds, Outcome outcome) {
        this.seat = Objects.requireNonNull(seat, "seat");
        this.playerType = Objects.requireNonNull(playerType, "playerType");
        this.profile = profile;
        this.displayName = requireDisplayName(displayName);
        this.score = requireNonNegative(score, "score");
        this.thinkingSeconds = requireNonNegative(thinkingSeconds, "thinkingSeconds");
        this.outcome = Objects.requireNonNull(outcome, "outcome");

        if (playerType == PlayerType.PROFILE && profile == null) {
            throw new IllegalArgumentException("A profile result must reference a profile.");
        }

        if (playerType != PlayerType.PROFILE && profile != null) {
            throw new IllegalArgumentException("Only a profile result may reference a profile.");
        }
    }

    public Seat seat() {
        return seat;
    }

    public PlayerType playerType() {
        return playerType;
    }

    public PlayerProfile profile() {
        return profile;
    }

    public boolean hasProfile() {
        return profile != null;
    }

    public String displayName() {
        return displayName;
    }

    public int score() {
        return score;
    }

    public int thinkingSeconds() {
        return thinkingSeconds;
    }

    public Outcome outcome() {
        return outcome;
    }

    private static String requireDisplayName(String displayName) {
        String normalized = Objects.requireNonNull(displayName, "displayName").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Display name must not be empty.");
        }

        return normalized;
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative.");
        }

        return value;
    }
}
