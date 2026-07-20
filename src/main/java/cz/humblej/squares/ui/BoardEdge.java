package cz.humblej.squares.ui;

final class BoardEdge {
    final boolean horizontal;
    final int rowOrLine;
    final int columnOrLine;

    BoardEdge(boolean horizontal, int rowOrLine, int columnOrLine) {
        this.horizontal = horizontal;
        this.rowOrLine = rowOrLine;
        this.columnOrLine = columnOrLine;
    }

    static boolean same(BoardEdge first, BoardEdge second) {
        if (first == null || second == null) {
            return first == second;
        }

        return first.horizontal == second.horizontal
                && first.rowOrLine == second.rowOrLine
                && first.columnOrLine == second.columnOrLine;
    }
}
