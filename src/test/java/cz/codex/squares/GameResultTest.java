package cz.codex.squares;

import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GameResultTest {
    private static final Instant STARTED_AT = Instant.parse("2026-07-16T12:00:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-07-16T12:05:00Z");

    @Test
    public void completedGameExposesWinnerAndStructuredValues() {
        PlayerProfile redProfile = new PlayerProfile(UUID.randomUUID(), "Jana", STARTED_AT, false);
        PlayerProfile blueProfile = new PlayerProfile(UUID.randomUUID(), "Petr", STARTED_AT, false);
        PlayerResult red = PlayerResult.forProfile(PlayerResult.Seat.RED, redProfile,
                18, 80, PlayerResult.Outcome.WIN);
        PlayerResult blue = PlayerResult.forProfile(PlayerResult.Seat.BLUE, blueProfile,
                12, 95, PlayerResult.Outcome.LOSS);
        UUID gameId = UUID.randomUUID();

        GameResult result = new GameResult(gameId, STARTED_AT, FINISHED_AT,
                GameResult.Mode.NETWORK, GameResult.FinishReason.BOARD_FULL,
                6, 5, 120, 300, true, null, red, blue);

        assertEquals(gameId, result.gameId());
        assertEquals(GameResult.Mode.NETWORK, result.mode());
        assertEquals(GameResult.FinishReason.BOARD_FULL, result.finishReason());
        assertEquals(6, result.rows());
        assertEquals(5, result.columns());
        assertEquals(120, result.thinkingTimeLimitSeconds());
        assertEquals(300, result.totalSeconds());
        assertTrue(result.randomInitialEdges());
        assertNull(result.cpuDifficulty());
        assertFalse(result.isDraw());
        assertSame(red, result.winner());
        assertEquals("\u010cerven\u00fd hr\u00e1\u010d v\u00edt\u011bz\u00ed 18:12.", Messages.gameOver(result));
    }

    @Test
    public void timeLimitResultUsesStructuredOutcomeInsteadOfScore() {
        PlayerResult red = PlayerResult.guest(PlayerResult.Seat.RED, Messages.PLAYER_RED,
                3, 20, PlayerResult.Outcome.WIN);
        PlayerResult blue = PlayerResult.guest(PlayerResult.Seat.BLUE, Messages.PLAYER_BLUE,
                8, 30, PlayerResult.Outcome.LOSS);
        GameResult result = new GameResult(UUID.randomUUID(), STARTED_AT, FINISHED_AT,
                GameResult.Mode.LOCAL, GameResult.FinishReason.TIME_LIMIT,
                5, 5, 30, 50, false, null, red, blue);

        assertSame(red, result.winner());
        assertEquals(Messages.blueLostOnTime(), Messages.gameOver(result));
    }

    @Test
    public void drawHasNoWinner() {
        PlayerResult red = PlayerResult.guest(PlayerResult.Seat.RED, Messages.PLAYER_RED,
                8, 40, PlayerResult.Outcome.DRAW);
        PlayerResult blue = PlayerResult.guest(PlayerResult.Seat.BLUE, Messages.PLAYER_BLUE,
                8, 35, PlayerResult.Outcome.DRAW);
        GameResult result = new GameResult(UUID.randomUUID(), STARTED_AT, FINISHED_AT,
                GameResult.Mode.LOCAL, GameResult.FinishReason.BOARD_FULL,
                4, 4, 0, 75, false, null, red, blue);

        assertTrue(result.isDraw());
        assertNull(result.winner());
        assertEquals("Rem\u00edza 8:8.", Messages.gameOver(result));
    }
}
