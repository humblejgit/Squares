package cz.humblej.squares.ui;

import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SquaresPanelComponentsTest {
    @Test
    public void stateCodecPreservesMatricesEdgeAndTimes() {
        int[][] source = {{0, 1}, {2, 3}};
        int[][] target = {{0, 0}, {0, 0}};

        SquaresPanelStateCodec.decodeMatrix(SquaresPanelStateCodec.encodeMatrix(source), target);
        assertArrayEquals(source[0], target[0]);
        assertArrayEquals(source[1], target[1]);

        BoardEdge edge = SquaresPanelStateCodec.decodeEdge(
                SquaresPanelStateCodec.encodeEdge(new BoardEdge(true, 2, 3)));
        assertNotNull(edge);
        assertEquals(true, edge.horizontal);
        assertEquals(2, edge.rowOrLine);
        assertEquals(3, edge.columnOrLine);
        assertNull(SquaresPanelStateCodec.decodeEdge("N"));
        assertArrayEquals(new int[]{12, 8, 4, 30},
                SquaresPanelStateCodec.decodeTimes(SquaresPanelStateCodec.encodeTimes(12, 8, 4, 30)));
    }

    @Test
    public void edgeLocatorKeepsClicksInsidePlayableInternalEdges() {
        BoardEdge horizontal = BoardEdgeLocator.find(30, 72, 24, 24, 5, 5, 48, 8);
        assertNotNull(horizontal);
        assertEquals(true, horizontal.horizontal);
        assertEquals(1, horizontal.rowOrLine);
        assertEquals(0, horizontal.columnOrLine);

        assertNull(BoardEdgeLocator.find(0, 0, 24, 24, 5, 5, 48, 8));
    }

    @Test
    public void panelKeepsTheExistingNetworkStateFormatWhileUsingTheCore() throws Exception {
        AtomicReference<String> encoded = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            SoundPlayer.setEnabled(false);
            SquaresPanel source = new SquaresPanel(2, 2);
            source.setClockEnabled(false);
            source.resetGame();
            source.applyMove(true, 1, 0);
            source.applyMove(true, 1, 1);
            encoded.set(source.encodeState());

            SquaresPanel restored = new SquaresPanel(2, 2);
            restored.setClockEnabled(false);
            restored.applyEncodedState(encoded.get());
            assertEquals(encoded.get(), restored.encodeState());
        });

        assertEquals("1|001200|000000|0000|H,1,1|0,0,0,0", encoded.get());
    }

}
