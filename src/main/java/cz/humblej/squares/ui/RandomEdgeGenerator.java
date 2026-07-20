package cz.humblej.squares.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RandomEdgeGenerator {
    private static final int NEUTRAL_EDGE = 3;

    private RandomEdgeGenerator() {
    }

    static void generate(int rows, int columns, int[][] horizontalEdges, int[][] verticalEdges) {
        List<BoardEdge> candidates = new ArrayList<>();
        for (int row = 1; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                candidates.add(new BoardEdge(true, row, column));
            }
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 1; column < columns; column++) {
                candidates.add(new BoardEdge(false, row, column));
            }
        }

        Collections.shuffle(candidates);
        int targetCount = Math.min(candidates.size(), Math.max(1, rows * columns / 3));
        int generatedCount = 0;
        for (BoardEdge edge : candidates) {
            if (generatedCount >= targetCount) {
                return;
            }
            if (canAdd(edge, rows, columns, horizontalEdges, verticalEdges)) {
                setOwner(edge, NEUTRAL_EDGE, horizontalEdges, verticalEdges);
                generatedCount++;
            }
        }
    }

    private static boolean canAdd(BoardEdge edge, int rows, int columns,
                                  int[][] horizontalEdges, int[][] verticalEdges) {
        setOwner(edge, NEUTRAL_EDGE, horizontalEdges, verticalEdges);
        boolean almostCompleted = edge.horizontal
                ? hasAtLeastThree(edge.rowOrLine - 1, edge.columnOrLine, rows, columns,
                        horizontalEdges, verticalEdges)
                    || hasAtLeastThree(edge.rowOrLine, edge.columnOrLine, rows, columns,
                        horizontalEdges, verticalEdges)
                : hasAtLeastThree(edge.rowOrLine, edge.columnOrLine - 1, rows, columns,
                        horizontalEdges, verticalEdges)
                    || hasAtLeastThree(edge.rowOrLine, edge.columnOrLine, rows, columns,
                        horizontalEdges, verticalEdges);
        setOwner(edge, SquaresPanel.NO_PLAYER, horizontalEdges, verticalEdges);
        return !almostCompleted;
    }

    private static boolean hasAtLeastThree(int row, int column, int rows, int columns,
                                           int[][] horizontalEdges, int[][] verticalEdges) {
        return row >= 0 && row < rows && column >= 0 && column < columns
                && countCellEdges(row, column, rows, columns, horizontalEdges, verticalEdges) >= 3;
    }

    private static int countCellEdges(int row, int column, int rows, int columns,
                                      int[][] horizontalEdges, int[][] verticalEdges) {
        int count = 0;
        if (row == 0 || horizontalEdges[row][column] != SquaresPanel.NO_PLAYER) {
            count++;
        }
        if (row + 1 == rows || horizontalEdges[row + 1][column] != SquaresPanel.NO_PLAYER) {
            count++;
        }
        if (column == 0 || verticalEdges[row][column] != SquaresPanel.NO_PLAYER) {
            count++;
        }
        if (column + 1 == columns || verticalEdges[row][column + 1] != SquaresPanel.NO_PLAYER) {
            count++;
        }
        return count;
    }

    private static void setOwner(BoardEdge edge, int owner,
                                 int[][] horizontalEdges, int[][] verticalEdges) {
        if (edge.horizontal) {
            horizontalEdges[edge.rowOrLine][edge.columnOrLine] = owner;
        } else {
            verticalEdges[edge.rowOrLine][edge.columnOrLine] = owner;
        }
    }
}
