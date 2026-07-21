package cz.humblej.squares.ui;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.model.PlayerResult;

import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class SquaresPanelGameResultTest {
    @Test
    public void fullBoardEmitsStructuredResultWithProfiles() throws Exception {
        AtomicReference<GameResult> captured = new AtomicReference<>();
        AtomicReference<PlayerProfile> redProfile = new AtomicReference<>();
        AtomicReference<PlayerProfile> blueProfile = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            SoundPlayer.setEnabled(false);
            PlayerProfile red = PlayerProfile.create("Jana");
            PlayerProfile blue = PlayerProfile.create("Petr");
            redProfile.set(red);
            blueProfile.set(blue);

            SquaresPanel panel = new SquaresPanel(2, 2);
            panel.setClockEnabled(false);
            panel.setPlayerProfiles(red, blue);
            panel.setGameOverHandler(captured::set);
            panel.resetGame();

            panel.applyMove(true, 1, 0);
            panel.applyMove(true, 1, 1);
            panel.applyMove(false, 0, 1);
            panel.applyMove(false, 1, 1);
        });

        GameResult result = captured.get();
        assertNotNull(result);
        assertNotNull(result.gameId());
        assertEquals(GameResult.Mode.LOCAL, result.mode());
        assertEquals(GameResult.FinishReason.BOARD_FULL, result.finishReason());
        assertEquals(2, result.rows());
        assertEquals(2, result.columns());
        assertEquals(4, result.redPlayer().score());
        assertEquals(0, result.bluePlayer().score());
        assertEquals(PlayerResult.Outcome.WIN, result.redPlayer().outcome());
        assertEquals(PlayerResult.Outcome.LOSS, result.bluePlayer().outcome());
        assertSame(redProfile.get(), result.redPlayer().profile());
        assertSame(blueProfile.get(), result.bluePlayer().profile());
    }
}
