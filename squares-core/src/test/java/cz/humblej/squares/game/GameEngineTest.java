package cz.humblej.squares.game;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.model.PlayerResult;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GameEngineTest {
    @Test
    public void appliesMovesClaimsCellsAndKeepsTurnAfterScoring() {
        GameEngine engine = new GameEngine(2, 2);

        assertTrue(engine.applyMove(new GameMove(true, 1, 0)).applied());
        assertEquals(GameEngine.BLUE_PLAYER, engine.currentPlayer());
        assertFalse(engine.applyMove(new GameMove(true, 1, 0)).applied());
        assertTrue(engine.applyMove(new GameMove(true, 1, 1)).applied());

        GameEngine.MoveResult firstCapture = engine.applyMove(new GameMove(false, 0, 1));
        assertEquals(2, firstCapture.completedCells());
        assertEquals(GameEngine.RED_PLAYER, engine.currentPlayer());

        GameEngine.MoveResult finalCapture = engine.applyMove(new GameMove(false, 1, 1));
        assertEquals(2, finalCapture.completedCells());
        assertTrue(finalCapture.gameOver());
        assertEquals(4, engine.score(GameEngine.RED_PLAYER));
        assertEquals(0, engine.score(GameEngine.BLUE_PLAYER));
    }

    @Test
    public void clockAndResultCreationDoNotDependOnSwing() {
        GameEngine engine = new GameEngine(2, 2);
        engine.setThinkingTimeLimitSeconds(2);

        assertFalse(engine.tickClock().timeExpired());
        GameEngine.TickResult expired = engine.tickClock();
        assertTrue(expired.timeExpired());
        assertEquals(GameEngine.RED_PLAYER, expired.timedOutPlayer());

        PlayerProfile red = PlayerProfile.create("Jana");
        GameResult result = GameResultFactory.create(engine, GameResult.Mode.COMPUTER,
                GameResult.FinishReason.TIME_LIMIT, expired.timedOutPlayer(), false,
                GameResult.CpuDifficulty.MEDIUM, GameParticipant.profile(red),
                GameParticipant.computer("CPU"));

        assertNotNull(result.gameId());
        assertSame(red, result.redPlayer().profile());
        assertEquals(PlayerResult.Outcome.LOSS, result.redPlayer().outcome());
        assertEquals(PlayerResult.Outcome.WIN, result.bluePlayer().outcome());
        assertEquals(2, result.redPlayer().thinkingSeconds());
    }

    @Test
    public void snapshotRestoresBoardTurnAndTimes() {
        GameEngine source = new GameEngine(3, 3);
        source.applyMove(new GameMove(true, 1, 0));
        source.tickClock();
        GameSnapshot snapshot = source.snapshot();

        GameEngine restored = new GameEngine(3, 3);
        restored.restore(snapshot);
        GameSnapshot restoredSnapshot = restored.snapshot();

        assertEquals(snapshot.currentPlayer(), restoredSnapshot.currentPlayer());
        assertEquals(snapshot.totalSeconds(), restoredSnapshot.totalSeconds());
        assertEquals(snapshot.lastMove(), restoredSnapshot.lastMove());
        assertMatricesEqual(snapshot.horizontalEdges(), restoredSnapshot.horizontalEdges());
        assertMatricesEqual(snapshot.verticalEdges(), restoredSnapshot.verticalEdges());
        assertMatricesEqual(snapshot.completedCells(), restoredSnapshot.completedCells());
    }

    @Test
    public void randomSetupNeverCreatesAThreeSidedCell() {
        int rows = 8;
        int columns = 8;
        GameEngine engine = new GameEngine(rows, columns);
        engine.reset(true, new Random(1234L));
        GameSnapshot snapshot = engine.snapshot();
        int[][] horizontal = snapshot.horizontalEdges();
        int[][] vertical = snapshot.verticalEdges();

        assertTrue(engine.selectedEdgeCount() > 0);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int count = (row == 0 || horizontal[row][column] != 0 ? 1 : 0)
                        + (row + 1 == rows || horizontal[row + 1][column] != 0 ? 1 : 0)
                        + (column == 0 || vertical[row][column] != 0 ? 1 : 0)
                        + (column + 1 == columns || vertical[row][column + 1] != 0 ? 1 : 0);
                assertTrue("Random setup created a nearly completed cell", count < 3);
            }
        }
    }

    private static void assertMatricesEqual(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(expected[row], actual[row]);
        }
    }
}
