package cz.humblej.squares.game;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Platform-neutral owner of the board, turn rules, scores and game clocks. */
public final class GameEngine {
    public static final int NO_PLAYER = 0;
    public static final int RED_PLAYER = 1;
    public static final int BLUE_PLAYER = 2;
    public static final int NEUTRAL_EDGE = 3;

    private int rows;
    private int columns;
    private int[][] horizontalEdges;
    private int[][] verticalEdges;
    private int[][] completedCells;
    private int currentPlayer;
    private GameMove lastMove;
    private int totalSeconds;
    private int redThinkingSeconds;
    private int blueThinkingSeconds;
    private int thinkingTimeLimitSeconds;
    private boolean gameOver;
    private UUID gameId;
    private Instant gameStartedAt;

    public GameEngine(int rows, int columns) {
        allocateBoard(rows, columns);
        reset(false, new Random());
    }

    public MoveResult applyMove(GameMove move) {
        if (gameOver || move == null || !isPlayable(move) || isSelected(move)) {
            return MoveResult.rejected();
        }

        setEdgeOwner(move, currentPlayer);
        lastMove = move;
        int completedCount = claimCompletedCellsAround(move);
        if (completedCount == 0) {
            currentPlayer = otherPlayer(currentPlayer);
        }
        gameOver = isBoardFull();
        return new MoveResult(true, completedCount, gameOver);
    }

    public TickResult tickClock() {
        if (gameOver) {
            return TickResult.unchanged();
        }

        totalSeconds++;
        boolean expired = false;
        if (hasThinkingTimeLimit()) {
            if (currentPlayer == RED_PLAYER) {
                redThinkingSeconds = Math.max(0, redThinkingSeconds - 1);
                expired = redThinkingSeconds == 0;
            } else {
                blueThinkingSeconds = Math.max(0, blueThinkingSeconds - 1);
                expired = blueThinkingSeconds == 0;
            }
        } else if (currentPlayer == RED_PLAYER) {
            redThinkingSeconds++;
        } else {
            blueThinkingSeconds++;
        }

        if (expired) {
            gameOver = true;
        }
        return new TickResult(true, expired, expired ? currentPlayer : NO_PLAYER);
    }

    public void reset(boolean randomInitialEdges, Random random) {
        clear(horizontalEdges);
        clear(verticalEdges);
        clear(completedCells);
        currentPlayer = RED_PLAYER;
        lastMove = null;
        gameOver = false;
        gameId = UUID.randomUUID();
        gameStartedAt = Instant.now();
        initializeTimes();
        if (randomInitialEdges) {
            generateRandomInitialEdges(random == null ? new Random() : random);
        }
    }

    public void resize(int rows, int columns, boolean randomInitialEdges, Random random) {
        allocateBoard(rows, columns);
        reset(randomInitialEdges, random);
    }

    public void restore(GameSnapshot snapshot) {
        if (snapshot.rows() != rows || snapshot.columns() != columns) {
            throw new IllegalArgumentException("Snapshot dimensions do not match the board.");
        }
        horizontalEdges = snapshot.horizontalEdges();
        verticalEdges = snapshot.verticalEdges();
        completedCells = snapshot.completedCells();
        currentPlayer = snapshot.currentPlayer();
        lastMove = snapshot.lastMove();
        totalSeconds = snapshot.totalSeconds();
        redThinkingSeconds = snapshot.redThinkingSeconds();
        blueThinkingSeconds = snapshot.blueThinkingSeconds();
        thinkingTimeLimitSeconds = snapshot.thinkingTimeLimitSeconds();
        gameOver = isBoardFull();
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(rows, columns, horizontalEdges, verticalEdges, completedCells,
                currentPlayer, lastMove, totalSeconds, redThinkingSeconds, blueThinkingSeconds,
                thinkingTimeLimitSeconds);
    }

    public void setThinkingTimeLimitSeconds(int seconds) {
        thinkingTimeLimitSeconds = Math.max(0, seconds);
        initializeTimes();
    }

    public void resetClock() {
        initializeTimes();
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }

    public int currentPlayer() {
        return currentPlayer;
    }

    public int thinkingTimeLimitSeconds() {
        return thinkingTimeLimitSeconds;
    }

    public int totalSeconds() {
        return totalSeconds;
    }

    public int thinkingTimeUsedSeconds(int player) {
        int displayed = player == RED_PLAYER ? redThinkingSeconds : blueThinkingSeconds;
        return hasThinkingTimeLimit() ? Math.max(0, thinkingTimeLimitSeconds - displayed) : displayed;
    }

    public int score(int player) {
        int count = 0;
        for (int[] row : completedCells) {
            for (int owner : row) {
                if (owner == player) {
                    count++;
                }
            }
        }
        return count;
    }

    public int selectedEdgeCount() {
        int count = 0;
        for (int row = 1; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (horizontalEdges[row][column] != NO_PLAYER) {
                    count++;
                }
            }
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 1; column < columns; column++) {
                if (verticalEdges[row][column] != NO_PLAYER) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isSelected(GameMove move) {
        if (move == null || !isPlayable(move)) {
            return false;
        }
        return move.horizontal()
                ? horizontalEdges[move.rowOrLine()][move.columnOrLine()] != NO_PLAYER
                : verticalEdges[move.rowOrLine()][move.columnOrLine()] != NO_PLAYER;
    }

    public boolean gameOver() {
        return gameOver;
    }

    public UUID gameId() {
        return gameId;
    }

    public Instant gameStartedAt() {
        return gameStartedAt;
    }

    private void allocateBoard(int rows, int columns) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive.");
        }
        this.rows = rows;
        this.columns = columns;
        horizontalEdges = new int[rows + 1][columns];
        verticalEdges = new int[rows][columns + 1];
        completedCells = new int[rows][columns];
    }

    private boolean isPlayable(GameMove move) {
        if (move.horizontal()) {
            return move.rowOrLine() > 0 && move.rowOrLine() < rows
                    && move.columnOrLine() >= 0 && move.columnOrLine() < columns;
        }
        return move.rowOrLine() >= 0 && move.rowOrLine() < rows
                && move.columnOrLine() > 0 && move.columnOrLine() < columns;
    }

    private void setEdgeOwner(GameMove move, int player) {
        if (move.horizontal()) {
            horizontalEdges[move.rowOrLine()][move.columnOrLine()] = player;
        } else {
            verticalEdges[move.rowOrLine()][move.columnOrLine()] = player;
        }
    }

    private int claimCompletedCellsAround(GameMove move) {
        if (move.horizontal()) {
            return claimCell(move.rowOrLine() - 1, move.columnOrLine())
                    + claimCell(move.rowOrLine(), move.columnOrLine());
        }
        return claimCell(move.rowOrLine(), move.columnOrLine() - 1)
                + claimCell(move.rowOrLine(), move.columnOrLine());
    }

    private int claimCell(int row, int column) {
        if (row < 0 || row >= rows || column < 0 || column >= columns
                || completedCells[row][column] != NO_PLAYER || !isCompletedCell(row, column)) {
            return 0;
        }
        completedCells[row][column] = currentPlayer;
        return 1;
    }

    private boolean isCompletedCell(int row, int column) {
        return hasHorizontalEdge(row, column) && hasHorizontalEdge(row + 1, column)
                && hasVerticalEdge(row, column) && hasVerticalEdge(row, column + 1);
    }

    private boolean hasHorizontalEdge(int rowLine, int column) {
        return rowLine == 0 || rowLine == rows || horizontalEdges[rowLine][column] != NO_PLAYER;
    }

    private boolean hasVerticalEdge(int row, int columnLine) {
        return columnLine == 0 || columnLine == columns || verticalEdges[row][columnLine] != NO_PLAYER;
    }

    private boolean isBoardFull() {
        return score(RED_PLAYER) + score(BLUE_PLAYER) == rows * columns;
    }

    private boolean hasThinkingTimeLimit() {
        return thinkingTimeLimitSeconds > 0;
    }

    private void initializeTimes() {
        totalSeconds = 0;
        redThinkingSeconds = hasThinkingTimeLimit() ? thinkingTimeLimitSeconds : 0;
        blueThinkingSeconds = hasThinkingTimeLimit() ? thinkingTimeLimitSeconds : 0;
    }

    private void generateRandomInitialEdges(Random random) {
        List<GameMove> candidates = new ArrayList<>();
        for (int row = 1; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                candidates.add(new GameMove(true, row, column));
            }
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 1; column < columns; column++) {
                candidates.add(new GameMove(false, row, column));
            }
        }

        Collections.shuffle(candidates, random);
        int targetCount = Math.min(candidates.size(), Math.max(1, rows * columns / 3));
        int generated = 0;
        for (GameMove move : candidates) {
            if (generated >= targetCount) {
                return;
            }
            setEdgeOwner(move, NEUTRAL_EDGE);
            boolean almostCompleted = touchesCellWithAtLeastThreeEdges(move);
            if (almostCompleted) {
                setEdgeOwner(move, NO_PLAYER);
            } else {
                generated++;
            }
        }
    }

    private boolean touchesCellWithAtLeastThreeEdges(GameMove move) {
        if (move.horizontal()) {
            return hasAtLeastThree(move.rowOrLine() - 1, move.columnOrLine())
                    || hasAtLeastThree(move.rowOrLine(), move.columnOrLine());
        }
        return hasAtLeastThree(move.rowOrLine(), move.columnOrLine() - 1)
                || hasAtLeastThree(move.rowOrLine(), move.columnOrLine());
    }

    private boolean hasAtLeastThree(int row, int column) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            return false;
        }
        int count = (hasHorizontalEdge(row, column) ? 1 : 0)
                + (hasHorizontalEdge(row + 1, column) ? 1 : 0)
                + (hasVerticalEdge(row, column) ? 1 : 0)
                + (hasVerticalEdge(row, column + 1) ? 1 : 0);
        return count >= 3;
    }

    private static void clear(int[][] target) {
        for (int[] row : target) {
            java.util.Arrays.fill(row, NO_PLAYER);
        }
    }

    private static int otherPlayer(int player) {
        return player == RED_PLAYER ? BLUE_PLAYER : RED_PLAYER;
    }

    public static final class MoveResult {
        private static final MoveResult REJECTED = new MoveResult(false, 0, false);
        private final boolean applied;
        private final int completedCells;
        private final boolean gameOver;

        private MoveResult(boolean applied, int completedCells, boolean gameOver) {
            this.applied = applied;
            this.completedCells = completedCells;
            this.gameOver = gameOver;
        }

        private static MoveResult rejected() {
            return REJECTED;
        }

        public boolean applied() {
            return applied;
        }

        public int completedCells() {
            return completedCells;
        }

        public boolean gameOver() {
            return gameOver;
        }
    }

    public static final class TickResult {
        private static final TickResult UNCHANGED = new TickResult(false, false, NO_PLAYER);
        private final boolean changed;
        private final boolean timeExpired;
        private final int timedOutPlayer;

        private TickResult(boolean changed, boolean timeExpired, int timedOutPlayer) {
            this.changed = changed;
            this.timeExpired = timeExpired;
            this.timedOutPlayer = timedOutPlayer;
        }

        private static TickResult unchanged() {
            return UNCHANGED;
        }

        public boolean changed() {
            return changed;
        }

        public boolean timeExpired() {
            return timeExpired;
        }

        public int timedOutPlayer() {
            return timedOutPlayer;
        }
    }
}
