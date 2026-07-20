package cz.humblej.squares.ui;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void randomEdgesNeverPrepareAThreeSidedCell() {
        int rows = 8;
        int columns = 8;
        int[][] horizontal = new int[rows + 1][columns];
        int[][] vertical = new int[rows][columns + 1];
        RandomEdgeGenerator.generate(rows, columns, horizontal, vertical);

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int count = (row == 0 || horizontal[row][column] != 0 ? 1 : 0)
                        + (row + 1 == rows || horizontal[row + 1][column] != 0 ? 1 : 0)
                        + (column == 0 || vertical[row][column] != 0 ? 1 : 0)
                        + (column + 1 == columns || vertical[row][column + 1] != 0 ? 1 : 0);
                assertFalse("Random setup created a nearly completed cell", count >= 3);
            }
        }
    }
}
