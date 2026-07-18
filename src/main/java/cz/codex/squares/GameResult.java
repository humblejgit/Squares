package cz.codex.squares;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable, persistence-ready description of a completed game. */
final class GameResult {
    enum Mode {
        LOCAL,
        COMPUTER,
        NETWORK
    }

    enum FinishReason {
        BOARD_FULL,
        TIME_LIMIT
    }

    enum CpuDifficulty {
        EASY,
        MEDIUM,
        HARD
    }

    private final UUID gameId;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Mode mode;
    private final FinishReason finishReason;
    private final int rows;
    private final int columns;
    private final int thinkingTimeLimitSeconds;
    private final int totalSeconds;
    private final boolean randomInitialEdges;
    private final CpuDifficulty cpuDifficulty;
    private final PlayerResult redPlayer;
    private final PlayerResult bluePlayer;

    GameResult(UUID gameId, Instant startedAt, Instant finishedAt, Mode mode,
               FinishReason finishReason, int rows, int columns,
               int thinkingTimeLimitSeconds, int totalSeconds,
               boolean randomInitialEdges, CpuDifficulty cpuDifficulty,
               PlayerResult redPlayer, PlayerResult bluePlayer) {
        this.gameId = Objects.requireNonNull(gameId, "gameId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.finishReason = Objects.requireNonNull(finishReason, "finishReason");
        this.rows = requirePositive(rows, "rows");
        this.columns = requirePositive(columns, "columns");
        this.thinkingTimeLimitSeconds = requireNonNegative(thinkingTimeLimitSeconds,
                "thinkingTimeLimitSeconds");
        this.totalSeconds = requireNonNegative(totalSeconds, "totalSeconds");
        this.randomInitialEdges = randomInitialEdges;
        this.cpuDifficulty = cpuDifficulty;
        this.redPlayer = Objects.requireNonNull(redPlayer, "redPlayer");
        this.bluePlayer = Objects.requireNonNull(bluePlayer, "bluePlayer");

        if (finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt must not be before startedAt.");
        }

        if (redPlayer.seat() != PlayerResult.Seat.RED || bluePlayer.seat() != PlayerResult.Seat.BLUE) {
            throw new IllegalArgumentException("Player results must match their seats.");
        }

        if (mode == Mode.COMPUTER && cpuDifficulty == null) {
            throw new IllegalArgumentException("A computer game must specify CPU difficulty.");
        }

        if (mode != Mode.COMPUTER && cpuDifficulty != null) {
            throw new IllegalArgumentException("Only a computer game may specify CPU difficulty.");
        }

        validateOutcomes(redPlayer.outcome(), bluePlayer.outcome());
        validateParticipants(mode, redPlayer, bluePlayer);
        validateFinishReason(finishReason, redPlayer, bluePlayer);
    }

    UUID gameId() {
        return gameId;
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant finishedAt() {
        return finishedAt;
    }

    Mode mode() {
        return mode;
    }

    FinishReason finishReason() {
        return finishReason;
    }

    int rows() {
        return rows;
    }

    int columns() {
        return columns;
    }

    int thinkingTimeLimitSeconds() {
        return thinkingTimeLimitSeconds;
    }

    int totalSeconds() {
        return totalSeconds;
    }

    boolean randomInitialEdges() {
        return randomInitialEdges;
    }

    CpuDifficulty cpuDifficulty() {
        return cpuDifficulty;
    }

    PlayerResult redPlayer() {
        return redPlayer;
    }

    PlayerResult bluePlayer() {
        return bluePlayer;
    }

    boolean isDraw() {
        return redPlayer.outcome() == PlayerResult.Outcome.DRAW;
    }

    PlayerResult winner() {
        if (redPlayer.outcome() == PlayerResult.Outcome.WIN) {
            return redPlayer;
        }

        if (bluePlayer.outcome() == PlayerResult.Outcome.WIN) {
            return bluePlayer;
        }

        return null;
    }

    private static void validateOutcomes(PlayerResult.Outcome red, PlayerResult.Outcome blue) {
        boolean draw = red == PlayerResult.Outcome.DRAW && blue == PlayerResult.Outcome.DRAW;
        boolean redWins = red == PlayerResult.Outcome.WIN && blue == PlayerResult.Outcome.LOSS;
        boolean blueWins = red == PlayerResult.Outcome.LOSS && blue == PlayerResult.Outcome.WIN;

        if (!draw && !redWins && !blueWins) {
            throw new IllegalArgumentException("Player outcomes do not describe one valid game result.");
        }
    }

    private static void validateParticipants(Mode mode, PlayerResult red, PlayerResult blue) {
        int computerPlayers = 0;

        if (red.playerType() == PlayerResult.PlayerType.CPU) {
            computerPlayers++;
        }

        if (blue.playerType() == PlayerResult.PlayerType.CPU) {
            computerPlayers++;
        }

        if (mode == Mode.COMPUTER && computerPlayers != 1) {
            throw new IllegalArgumentException("A computer game must contain exactly one CPU player.");
        }

        if (mode != Mode.COMPUTER && computerPlayers != 0) {
            throw new IllegalArgumentException("Only a computer game may contain a CPU player.");
        }
    }

    private static void validateFinishReason(FinishReason finishReason,
                                             PlayerResult red, PlayerResult blue) {
        if (finishReason == FinishReason.TIME_LIMIT) {
            if (red.outcome() == PlayerResult.Outcome.DRAW) {
                throw new IllegalArgumentException("A time-limit result cannot be a draw.");
            }

            return;
        }

        PlayerResult.Outcome expectedRed;
        PlayerResult.Outcome expectedBlue;

        if (red.score() > blue.score()) {
            expectedRed = PlayerResult.Outcome.WIN;
            expectedBlue = PlayerResult.Outcome.LOSS;
        } else if (blue.score() > red.score()) {
            expectedRed = PlayerResult.Outcome.LOSS;
            expectedBlue = PlayerResult.Outcome.WIN;
        } else {
            expectedRed = PlayerResult.Outcome.DRAW;
            expectedBlue = PlayerResult.Outcome.DRAW;
        }

        if (red.outcome() != expectedRed || blue.outcome() != expectedBlue) {
            throw new IllegalArgumentException("A full-board outcome must match the player scores.");
        }
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive.");
        }

        return value;
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative.");
        }

        return value;
    }
}
