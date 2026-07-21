package cz.humblej.squares.game;

import java.util.Objects;

/** Immutable description of one playable board edge shared by all clients. */
public final class GameMove {
    private final boolean horizontal;
    private final int rowOrLine;
    private final int columnOrLine;

    public GameMove(boolean horizontal, int rowOrLine, int columnOrLine) {
        this.horizontal = horizontal;
        this.rowOrLine = rowOrLine;
        this.columnOrLine = columnOrLine;
    }

    public boolean horizontal() {
        return horizontal;
    }

    public int rowOrLine() {
        return rowOrLine;
    }

    public int columnOrLine() {
        return columnOrLine;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GameMove)) {
            return false;
        }
        GameMove move = (GameMove) other;
        return horizontal == move.horizontal
                && rowOrLine == move.rowOrLine
                && columnOrLine == move.columnOrLine;
    }

    @Override
    public int hashCode() {
        return Objects.hash(horizontal, rowOrLine, columnOrLine);
    }
}
