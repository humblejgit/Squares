package cz.humblej.squares.game;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerResult;

import java.time.Instant;
import java.util.Objects;

/** Creates persistence-ready results from the platform-neutral game state. */
public final class GameResultFactory {
    private GameResultFactory() {
    }

    public static GameResult create(GameEngine engine, GameResult.Mode mode,
                                    GameResult.FinishReason finishReason, int timedOutPlayer,
                                    boolean randomInitialEdges, GameResult.CpuDifficulty cpuDifficulty,
                                    GameParticipant redParticipant, GameParticipant blueParticipant) {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(finishReason, "finishReason");
        Objects.requireNonNull(redParticipant, "redParticipant");
        Objects.requireNonNull(blueParticipant, "blueParticipant");

        int redScore = engine.score(GameEngine.RED_PLAYER);
        int blueScore = engine.score(GameEngine.BLUE_PLAYER);
        PlayerResult.Outcome redOutcome;
        PlayerResult.Outcome blueOutcome;

        if (finishReason == GameResult.FinishReason.TIME_LIMIT) {
            if (timedOutPlayer != GameEngine.RED_PLAYER && timedOutPlayer != GameEngine.BLUE_PLAYER) {
                throw new IllegalArgumentException("A time-limit result must identify the timed-out player.");
            }
            redOutcome = timedOutPlayer == GameEngine.RED_PLAYER
                    ? PlayerResult.Outcome.LOSS : PlayerResult.Outcome.WIN;
            blueOutcome = timedOutPlayer == GameEngine.BLUE_PLAYER
                    ? PlayerResult.Outcome.LOSS : PlayerResult.Outcome.WIN;
        } else if (redScore > blueScore) {
            redOutcome = PlayerResult.Outcome.WIN;
            blueOutcome = PlayerResult.Outcome.LOSS;
        } else if (blueScore > redScore) {
            redOutcome = PlayerResult.Outcome.LOSS;
            blueOutcome = PlayerResult.Outcome.WIN;
        } else {
            redOutcome = PlayerResult.Outcome.DRAW;
            blueOutcome = PlayerResult.Outcome.DRAW;
        }

        PlayerResult red = createPlayerResult(engine, GameEngine.RED_PLAYER,
                redParticipant, redScore, redOutcome);
        PlayerResult blue = createPlayerResult(engine, GameEngine.BLUE_PLAYER,
                blueParticipant, blueScore, blueOutcome);

        return new GameResult(engine.gameId(), engine.gameStartedAt(), Instant.now(), mode,
                finishReason, engine.rows(), engine.columns(), engine.thinkingTimeLimitSeconds(),
                engine.totalSeconds(), randomInitialEdges, cpuDifficulty, red, blue);
    }

    private static PlayerResult createPlayerResult(GameEngine engine, int player,
                                                    GameParticipant participant, int score,
                                                    PlayerResult.Outcome outcome) {
        PlayerResult.Seat seat = player == GameEngine.RED_PLAYER
                ? PlayerResult.Seat.RED : PlayerResult.Seat.BLUE;
        int thinkingSeconds = engine.thinkingTimeUsedSeconds(player);

        if (participant.type() == GameParticipant.Type.PROFILE) {
            return PlayerResult.forProfile(seat, participant.profile(), score, thinkingSeconds, outcome);
        }
        if (participant.type() == GameParticipant.Type.COMPUTER) {
            return PlayerResult.computer(seat, participant.displayName(), score, thinkingSeconds, outcome);
        }
        return PlayerResult.guest(seat, participant.displayName(), score, thinkingSeconds, outcome);
    }
}
