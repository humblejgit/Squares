package cz.codex.squares;

import java.util.Objects;

/** Immutable aggregate of all locally recorded games for one local profile. */
final class LocalProfileStatistics {
    private final PlayerProfile profile;
    private final long games;
    private final long wins;
    private final long draws;
    private final long losses;
    private final long totalScore;

    LocalProfileStatistics(PlayerProfile profile, long games, long wins, long draws,
                           long losses, long totalScore) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.games = requireNonNegative(games, "games");
        this.wins = requireNonNegative(wins, "wins");
        this.draws = requireNonNegative(draws, "draws");
        this.losses = requireNonNegative(losses, "losses");
        this.totalScore = requireNonNegative(totalScore, "totalScore");

        if (games != wins + draws + losses) {
            throw new IllegalArgumentException("Games must equal wins, draws and losses.");
        }
    }

    PlayerProfile profile() {
        return profile;
    }

    long games() {
        return games;
    }

    long wins() {
        return wins;
    }

    long draws() {
        return draws;
    }

    long losses() {
        return losses;
    }

    long totalScore() {
        return totalScore;
    }

    double winPercentage() {
        return games == 0L ? 0.0 : wins * 100.0 / games;
    }

    private static long requireNonNegative(long value, String field) {
        if (value < 0L) {
            throw new IllegalArgumentException(field + " must not be negative.");
        }
        return value;
    }
}
