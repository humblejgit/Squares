package cz.humblej.squares.game;

/** Immutable state used by renderers, network codecs and future clients. */
public final class GameSnapshot {
    private final int rows;
    private final int columns;
    private final int[][] horizontalEdges;
    private final int[][] verticalEdges;
    private final int[][] completedCells;
    private final int currentPlayer;
    private final GameMove lastMove;
    private final int totalSeconds;
    private final int redThinkingSeconds;
    private final int blueThinkingSeconds;
    private final int thinkingTimeLimitSeconds;

    public GameSnapshot(int rows, int columns, int[][] horizontalEdges, int[][] verticalEdges,
                        int[][] completedCells, int currentPlayer, GameMove lastMove,
                        int totalSeconds, int redThinkingSeconds, int blueThinkingSeconds,
                        int thinkingTimeLimitSeconds) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive.");
        }
        requireDimensions(horizontalEdges, rows + 1, columns, "horizontalEdges");
        requireDimensions(verticalEdges, rows, columns + 1, "verticalEdges");
        requireDimensions(completedCells, rows, columns, "completedCells");
        if (currentPlayer != GameEngine.RED_PLAYER && currentPlayer != GameEngine.BLUE_PLAYER) {
            throw new IllegalArgumentException("Unknown current player.");
        }

        this.rows = rows;
        this.columns = columns;
        this.horizontalEdges = copy(horizontalEdges);
        this.verticalEdges = copy(verticalEdges);
        this.completedCells = copy(completedCells);
        this.currentPlayer = currentPlayer;
        this.lastMove = lastMove;
        this.totalSeconds = requireNonNegative(totalSeconds, "totalSeconds");
        this.redThinkingSeconds = requireNonNegative(redThinkingSeconds, "redThinkingSeconds");
        this.blueThinkingSeconds = requireNonNegative(blueThinkingSeconds, "blueThinkingSeconds");
        this.thinkingTimeLimitSeconds = requireNonNegative(thinkingTimeLimitSeconds,
                "thinkingTimeLimitSeconds");
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }

    public int[][] horizontalEdges() {
        return copy(horizontalEdges);
    }

    public int[][] verticalEdges() {
        return copy(verticalEdges);
    }

    public int[][] completedCells() {
        return copy(completedCells);
    }

    public int currentPlayer() {
        return currentPlayer;
    }

    public GameMove lastMove() {
        return lastMove;
    }

    public int totalSeconds() {
        return totalSeconds;
    }

    public int redThinkingSeconds() {
        return redThinkingSeconds;
    }

    public int blueThinkingSeconds() {
        return blueThinkingSeconds;
    }

    public int thinkingTimeLimitSeconds() {
        return thinkingTimeLimitSeconds;
    }

    private static void requireDimensions(int[][] values, int expectedRows, int expectedColumns, String name) {
        if (values == null || values.length != expectedRows) {
            throw new IllegalArgumentException("Invalid " + name + " row count.");
        }
        for (int[] row : values) {
            if (row == null || row.length != expectedColumns) {
                throw new IllegalArgumentException("Invalid " + name + " column count.");
            }
        }
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative.");
        }
        return value;
    }

    private static int[][] copy(int[][] source) {
        int[][] result = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            result[row] = source[row].clone();
        }
        return result;
    }
}
