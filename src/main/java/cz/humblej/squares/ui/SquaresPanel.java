package cz.humblej.squares.ui;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.model.PlayerResult;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public final class SquaresPanel extends JPanel {
    static final int CELL_SIZE = 48;
    static final int PADDING = 24;
    static final int STATUS_HEIGHT = 66;
    static final int INFO_HEIGHT = 72;
    private static final int RESTART_BUTTON_WIDTH = 76;
    private static final int RESTART_BUTTON_HEIGHT = 30;
    private static final int CLICK_TOLERANCE = 8;

    public static final int NO_PLAYER = 0;
    public static final int RED_PLAYER = 1;
    public static final int BLUE_PLAYER = 2;

    private static final Color BACKGROUND = new Color(248, 249, 250);

    private int rows;
    private int columns;
    private int[][] horizontalEdges;
    private int[][] verticalEdges;
    private int[][] completedCells;
    private final JButton restartButton;
    private BoardEdge hoveredEdge;
    private MoveHandler moveHandler;
    private GameOverHandler gameOverHandler;
    private RestartHandler restartHandler;
    private ClockTickHandler clockTickHandler;
    private int localPlayer = NO_PLAYER;
    private int currentPlayer = RED_PLAYER;
    private BoardEdge lastMove;
    private int totalSeconds;
    private int redThinkingSeconds;
    private int blueThinkingSeconds;
    private int thinkingTimeLimitSeconds;
    private String networkInfo = "";
    private boolean gameOverAnnounced;
    private boolean clockEnabled = true;
    private boolean clockPausedByDialog;
    private boolean windowActive = true;
    private boolean randomInitialEdgesEnabled;
    private int computerPlayer = NO_PLAYER;
    private ComputerDifficulty computerDifficulty = ComputerDifficulty.MEDIUM;
    private PlayerProfile redProfile;
    private PlayerProfile blueProfile;
    private UUID gameId = UUID.randomUUID();
    private Instant gameStartedAt = Instant.now();
    private final Random random = new Random();
    private final ComputerMoveController computerMoves = new ComputerMoveController();

    public SquaresPanel(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.horizontalEdges = new int[rows + 1][columns];
        this.verticalEdges = new int[rows][columns + 1];
        this.completedCells = new int[rows][columns];

        int width = columns * CELL_SIZE + PADDING * 2;
        int height = rows * CELL_SIZE + PADDING * 2 + STATUS_HEIGHT + INFO_HEIGHT;
        setPreferredSize(new Dimension(width, height));
        setBackground(BACKGROUND);
        setLayout(null);

        restartButton = new JButton(Messages.RESTART_BUTTON);
        restartButton.setFocusable(false);
        restartButton.setMargin(new java.awt.Insets(2, 6, 2, 6));
        restartButton.addActionListener(event -> requestRestart());
        add(restartButton);

        Timer clockTimer = new Timer(1000, event -> updateClock());
        clockTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                selectEdgeAt(event.getX(), event.getY());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                setHoveredEdge(null);
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                setHoveredEdge(findEdgeAt(event.getX(), event.getY()));
            }
        });

        SoundPlayer.gameStart();
    }

    @Override
    public void doLayout() {
        super.doLayout();

        int x = gridLeft() + ((columns * CELL_SIZE) - RESTART_BUTTON_WIDTH) / 2;
        int y = PADDING + 2;
        restartButton.setBounds(x, y, RESTART_BUTTON_WIDTH, RESTART_BUTTON_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            SquaresPanelPainter.paint(g2, new SquaresPanelPainter.State(
                    rows, columns, horizontalEdges, verticalEdges, completedCells,
                    hoveredEdge, lastMove, localPlayer, currentPlayer,
                    totalSeconds, redThinkingSeconds, blueThinkingSeconds, networkInfo));
        } finally {
            g2.dispose();
        }
    }

    private void selectEdgeAt(int mouseX, int mouseY) {
        BoardEdge edge = findEdgeAt(mouseX, mouseY);

        if (edge == null || isSelected(edge)) {
            return;
        }

        if (localPlayer != NO_PLAYER && currentPlayer != localPlayer) {
            return;
        }

        if (moveHandler != null) {
            moveHandler.moveRequested(edge.horizontal, edge.rowOrLine, edge.columnOrLine);
            return;
        }

        applyMove(edge.horizontal, edge.rowOrLine, edge.columnOrLine);
    }

    public boolean applyMove(boolean horizontal, int rowOrLine, int columnOrLine) {
        if (gameOverAnnounced) {
            return false;
        }

        BoardEdge edge = new BoardEdge(horizontal, rowOrLine, columnOrLine);

        if (isSelected(edge)) {
            return false;
        }

        setEdgeOwner(edge, currentPlayer);
        lastMove = edge;
        int completedCount = claimCompletedCellsAround(edge);

        if (completedCount == 0) {
            switchPlayer();
            SoundPlayer.edge();
        } else {
            SoundPlayer.square();
        }

        repaint();
        checkGameOver();
        scheduleComputerMoveIfNeeded();
        return true;
    }

    public void setMoveHandler(MoveHandler moveHandler) {
        this.moveHandler = moveHandler;
    }

    public void setGameOverHandler(GameOverHandler gameOverHandler) {
        this.gameOverHandler = gameOverHandler;
    }

    public void setRestartHandler(RestartHandler restartHandler) {
        this.restartHandler = restartHandler;
    }

    public void setClockTickHandler(ClockTickHandler clockTickHandler) {
        this.clockTickHandler = clockTickHandler;
    }

    public void setLocalPlayer(int localPlayer) {
        this.localPlayer = localPlayer;
    }

    public void setPlayerProfiles(PlayerProfile redProfile, PlayerProfile blueProfile) {
        if (redProfile != null && redProfile.equals(blueProfile)) {
            throw new IllegalArgumentException("One profile cannot occupy both seats.");
        }

        this.redProfile = redProfile;
        this.blueProfile = blueProfile;
    }

    public PlayerProfile redPlayerProfile() {
        return redProfile;
    }

    public PlayerProfile bluePlayerProfile() {
        return blueProfile;
    }

    public String redPlayerDisplayName() {
        return redProfile == null ? Messages.PLAYER_RED : redProfile.displayName();
    }

    public String bluePlayerDisplayName() {
        return blueProfile == null ? Messages.PLAYER_GUEST : blueProfile.displayName();
    }

    public void setComputerOpponent(ComputerDifficulty difficulty) {
        this.computerPlayer = BLUE_PLAYER;
        this.computerDifficulty = difficulty == null ? ComputerDifficulty.MEDIUM : difficulty;
        setLocalPlayer(RED_PLAYER);
        scheduleComputerMoveIfNeeded();
    }

    public void clearComputerOpponent() {
        this.computerPlayer = NO_PLAYER;
        stopComputerMoveTimer();
        if (localPlayer == RED_PLAYER) {
            setLocalPlayer(NO_PLAYER);
        }
    }

    public ComputerDifficulty computerDifficulty() {
        return computerDifficulty;
    }

    public void setNetworkInfo(String networkInfo) {
        this.networkInfo = networkInfo == null ? "" : networkInfo;
        repaint();
    }

    public void setClockEnabled(boolean clockEnabled) {
        this.clockEnabled = clockEnabled;
    }

    public void resetClock() {
        initializeTimes();
        repaint();
    }

    public void setClockPausedByDialog(boolean clockPausedByDialog) {
        this.clockPausedByDialog = clockPausedByDialog;
    }

    public void setWindowActive(boolean windowActive) {
        this.windowActive = windowActive;
    }

    public void setThinkingTimeLimitSeconds(int thinkingTimeLimitSeconds) {
        this.thinkingTimeLimitSeconds = Math.max(0, thinkingTimeLimitSeconds);
        initializeTimes();
        repaint();
    }

    public void setRandomInitialEdgesEnabled(boolean randomInitialEdgesEnabled) {
        this.randomInitialEdgesEnabled = randomInitialEdgesEnabled;
    }

    public boolean randomInitialEdgesEnabled() {
        return randomInitialEdgesEnabled;
    }

    public String networkInfo() {
        return networkInfo;
    }

    public void resetGame() {
        stopComputerMoveTimer();
        clear(horizontalEdges);
        clear(verticalEdges);
        clear(completedCells);
        hoveredEdge = null;
        lastMove = null;
        beginNewGame();
        initializeTimes();
        currentPlayer = RED_PLAYER;
        gameOverAnnounced = false;
        generateRandomInitialEdges();
        repaint();
        SoundPlayer.gameStart();
        scheduleComputerMoveIfNeeded();
    }

    public void resizeBoard(int rows, int columns) {
        stopComputerMoveTimer();
        this.rows = rows;
        this.columns = columns;
        this.horizontalEdges = new int[rows + 1][columns];
        this.verticalEdges = new int[rows][columns + 1];
        this.completedCells = new int[rows][columns];
        hoveredEdge = null;
        lastMove = null;
        beginNewGame();
        initializeTimes();
        currentPlayer = RED_PLAYER;
        gameOverAnnounced = false;
        generateRandomInitialEdges();

        int width = columns * CELL_SIZE + PADDING * 2;
        int height = rows * CELL_SIZE + PADDING * 2 + STATUS_HEIGHT + INFO_HEIGHT;
        setPreferredSize(new Dimension(width, height));
        revalidate();
        doLayout();
        repaint();
        SoundPlayer.gameStart();
        scheduleComputerMoveIfNeeded();
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int boardRows() {
        return rows;
    }

    public int boardColumns() {
        return columns;
    }

    public int thinkingTimeLimitSeconds() {
        return thinkingTimeLimitSeconds;
    }

    public String encodeState() {
        return currentPlayer + "|" + SquaresPanelStateCodec.encodeMatrix(horizontalEdges)
                + "|" + SquaresPanelStateCodec.encodeMatrix(verticalEdges)
                + "|" + SquaresPanelStateCodec.encodeMatrix(completedCells)
                + "|" + SquaresPanelStateCodec.encodeEdge(lastMove)
                + "|" + SquaresPanelStateCodec.encodeTimes(totalSeconds, redThinkingSeconds,
                        blueThinkingSeconds, thinkingTimeLimitSeconds);
    }

    public void applyEncodedState(String encodedState) {
        String[] parts = encodedState.split("\\|", -1);

        if (parts.length != 4 && parts.length != 5 && parts.length != 6) {
            return;
        }

        int edgeCountBefore = countSelectedEdges();
        int completedCountBefore = countCompletedCells(RED_PLAYER) + countCompletedCells(BLUE_PLAYER);
        currentPlayer = Integer.parseInt(parts[0]);
        SquaresPanelStateCodec.decodeMatrix(parts[1], horizontalEdges);
        SquaresPanelStateCodec.decodeMatrix(parts[2], verticalEdges);
        SquaresPanelStateCodec.decodeMatrix(parts[3], completedCells);
        lastMove = parts.length == 5 ? SquaresPanelStateCodec.decodeEdge(parts[4]) : null;
        if (parts.length == 6) {
            lastMove = SquaresPanelStateCodec.decodeEdge(parts[4]);
            int[] times = SquaresPanelStateCodec.decodeTimes(parts[5]);
            if (times != null) {
                totalSeconds = times[0];
                redThinkingSeconds = times[1];
                blueThinkingSeconds = times[2];
                if (times[3] >= 0) {
                    thinkingTimeLimitSeconds = times[3];
                }
            }
        }
        playStateChangeSound(edgeCountBefore, completedCountBefore);
        repaint();
        checkGameOver();
    }

    private void clear(int[][] target) {
        for (int row = 0; row < target.length; row++) {
            for (int column = 0; column < target[row].length; column++) {
                target[row][column] = NO_PLAYER;
            }
        }
    }

    private void generateRandomInitialEdges() {
        if (randomInitialEdgesEnabled) {
            RandomEdgeGenerator.generate(rows, columns, horizontalEdges, verticalEdges);
        }
    }

    private void requestRestart() {
        if (restartHandler != null) {
            restartHandler.restartRequested();
        }
    }

    private void updateClock() {
        if (!clockEnabled || clockPausedByDialog || !windowActive || gameOverAnnounced) {
            return;
        }

        totalSeconds++;
        boolean timeExpired = false;

        if (hasThinkingTimeLimit()) {
            if (currentPlayer == RED_PLAYER) {
                redThinkingSeconds = Math.max(0, redThinkingSeconds - 1);
                timeExpired = redThinkingSeconds == 0;
            } else if (currentPlayer == BLUE_PLAYER) {
                blueThinkingSeconds = Math.max(0, blueThinkingSeconds - 1);
                timeExpired = blueThinkingSeconds == 0;
            }
        } else if (currentPlayer == RED_PLAYER) {
            redThinkingSeconds++;
        } else if (currentPlayer == BLUE_PLAYER) {
            blueThinkingSeconds++;
        }

        repaint();

        if (clockTickHandler != null) {
            clockTickHandler.clockTick();
        }

        if (timeExpired) {
            announceTimeLoss();
        }
    }

    private void initializeTimes() {
        totalSeconds = 0;

        if (hasThinkingTimeLimit()) {
            redThinkingSeconds = thinkingTimeLimitSeconds;
            blueThinkingSeconds = thinkingTimeLimitSeconds;
        } else {
            redThinkingSeconds = 0;
            blueThinkingSeconds = 0;
        }
    }

    private boolean hasThinkingTimeLimit() {
        return thinkingTimeLimitSeconds > 0;
    }

    private void announceTimeLoss() {
        if (gameOverAnnounced) {
            return;
        }

        gameOverAnnounced = true;
        stopComputerMoveTimer();
        SoundPlayer.gameOver();

        if (gameOverHandler != null) {
            gameOverHandler.gameOver(createGameResult(GameResult.FinishReason.TIME_LIMIT, currentPlayer));
        }
    }

    private BoardEdge findEdgeAt(int mouseX, int mouseY) {
        return BoardEdgeLocator.find(mouseX, mouseY, gridLeft(), gridTop(),
                rows, columns, CELL_SIZE, CLICK_TOLERANCE);
    }

    private int claimCompletedCellsAround(BoardEdge edge) {
        int claimed = 0;

        if (edge.horizontal) {
            claimed += claimCell(edge.rowOrLine - 1, edge.columnOrLine);
            claimed += claimCell(edge.rowOrLine, edge.columnOrLine);
        } else {
            claimed += claimCell(edge.rowOrLine, edge.columnOrLine - 1);
            claimed += claimCell(edge.rowOrLine, edge.columnOrLine);
        }

        return claimed;
    }

    private int claimCell(int row, int column) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            return 0;
        }

        if (completedCells[row][column] != NO_PLAYER || !isCompletedCell(row, column)) {
            return 0;
        }

        completedCells[row][column] = currentPlayer;
        return 1;
    }

    private boolean isCompletedCell(int row, int column) {
        return hasHorizontalEdge(row, column)
                && hasHorizontalEdge(row + 1, column)
                && hasVerticalEdge(row, column)
                && hasVerticalEdge(row, column + 1);
    }

    private boolean hasHorizontalEdge(int rowLine, int column) {
        if (rowLine == 0 || rowLine == rows) {
            return true;
        }

        return horizontalEdges[rowLine][column] != NO_PLAYER;
    }

    private boolean hasVerticalEdge(int row, int columnLine) {
        if (columnLine == 0 || columnLine == columns) {
            return true;
        }

        return verticalEdges[row][columnLine] != NO_PLAYER;
    }

    private void setHoveredEdge(BoardEdge edge) {
        if (BoardEdge.same(hoveredEdge, edge)) {
            return;
        }

        hoveredEdge = edge;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    private boolean isSelected(BoardEdge edge) {
        if (edge.horizontal) {
            return horizontalEdges[edge.rowOrLine][edge.columnOrLine] != NO_PLAYER;
        }

        return verticalEdges[edge.rowOrLine][edge.columnOrLine] != NO_PLAYER;
    }

    private void setEdgeOwner(BoardEdge edge, int player) {
        if (edge.horizontal) {
            horizontalEdges[edge.rowOrLine][edge.columnOrLine] = player;
        } else {
            verticalEdges[edge.rowOrLine][edge.columnOrLine] = player;
        }
    }

    private void switchPlayer() {
        currentPlayer = currentPlayer == RED_PLAYER ? BLUE_PLAYER : RED_PLAYER;
    }

    private int countCompletedCells(int player) {
        int count = 0;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (completedCells[row][column] == player) {
                    count++;
                }
            }
        }

        return count;
    }

    private void checkGameOver() {
        if (gameOverAnnounced || !isBoardFull()) {
            return;
        }

        gameOverAnnounced = true;
        stopComputerMoveTimer();
        SoundPlayer.gameOver();

        if (gameOverHandler != null) {
            gameOverHandler.gameOver(createGameResult(GameResult.FinishReason.BOARD_FULL, NO_PLAYER));
        }
    }

    private void playStateChangeSound(int edgeCountBefore, int completedCountBefore) {
        int edgeCountAfter = countSelectedEdges();
        int completedCountAfter = countCompletedCells(RED_PLAYER) + countCompletedCells(BLUE_PLAYER);

        if (completedCountAfter > completedCountBefore) {
            SoundPlayer.square();
        } else if (edgeCountAfter > edgeCountBefore) {
            SoundPlayer.edge();
        }
    }

    private int countSelectedEdges() {
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

    private boolean isBoardFull() {
        return countCompletedCells(RED_PLAYER) + countCompletedCells(BLUE_PLAYER) == rows * columns;
    }

    private GameResult createGameResult(GameResult.FinishReason finishReason, int timedOutPlayer) {
        int redScore = countCompletedCells(RED_PLAYER);
        int blueScore = countCompletedCells(BLUE_PLAYER);
        PlayerResult.Outcome redOutcome;
        PlayerResult.Outcome blueOutcome;

        if (finishReason == GameResult.FinishReason.TIME_LIMIT) {
            redOutcome = timedOutPlayer == RED_PLAYER ? PlayerResult.Outcome.LOSS : PlayerResult.Outcome.WIN;
            blueOutcome = timedOutPlayer == BLUE_PLAYER ? PlayerResult.Outcome.LOSS : PlayerResult.Outcome.WIN;
        } else if (redScore > blueScore) {
            redOutcome = PlayerResult.Outcome.WIN;
            blueOutcome = PlayerResult.Outcome.LOSS;
        } else if (blueScore > redScore) {
            redOutcome = PlayerResult.Outcome.LOSS;
            blueOutcome = PlayerResult.Outcome.WIN;
        } else {
            redOutcome = PlayerResult.Outcome.DRAW;
            blueOutcome = PlayerResult.Outcome.DRAW;
        }

        PlayerResult redResult = createPlayerResult(RED_PLAYER, redScore, redOutcome);
        PlayerResult blueResult = createPlayerResult(BLUE_PLAYER, blueScore, blueOutcome);

        return new GameResult(gameId, gameStartedAt, Instant.now(), gameMode(), finishReason,
                rows, columns, thinkingTimeLimitSeconds, totalSeconds, randomInitialEdgesEnabled,
                gameResultCpuDifficulty(), redResult, blueResult);
    }

    private PlayerResult createPlayerResult(int player, int score, PlayerResult.Outcome outcome) {
        PlayerResult.Seat seat = player == RED_PLAYER ? PlayerResult.Seat.RED : PlayerResult.Seat.BLUE;
        PlayerProfile profile = player == RED_PLAYER ? redProfile : blueProfile;
        int thinkingSeconds = thinkingTimeUsedSeconds(player);

        if (computerPlayer == player) {
            return PlayerResult.computer(seat, Messages.PLAYER_CPU, score, thinkingSeconds, outcome);
        }

        if (profile != null) {
            return PlayerResult.forProfile(seat, profile, score, thinkingSeconds, outcome);
        }

        String displayName = player == RED_PLAYER ? redPlayerDisplayName() : bluePlayerDisplayName();
        return PlayerResult.guest(seat, displayName, score, thinkingSeconds, outcome);
    }

    private int thinkingTimeUsedSeconds(int player) {
        int displayedSeconds = player == RED_PLAYER ? redThinkingSeconds : blueThinkingSeconds;
        return hasThinkingTimeLimit()
                ? Math.max(0, thinkingTimeLimitSeconds - displayedSeconds)
                : displayedSeconds;
    }

    private GameResult.Mode gameMode() {
        if (computerPlayer != NO_PLAYER) {
            return GameResult.Mode.COMPUTER;
        }

        return localPlayer == NO_PLAYER ? GameResult.Mode.LOCAL : GameResult.Mode.NETWORK;
    }

    private GameResult.CpuDifficulty gameResultCpuDifficulty() {
        if (computerPlayer == NO_PLAYER) {
            return null;
        }

        return GameResult.CpuDifficulty.valueOf(computerDifficulty.name());
    }

    private void beginNewGame() {
        gameId = UUID.randomUUID();
        gameStartedAt = Instant.now();
    }

    private int gridLeft() {
        return PADDING;
    }

    private int gridTop() {
        return PADDING + STATUS_HEIGHT;
    }

    private void scheduleComputerMoveIfNeeded() {
        if (computerPlayer == NO_PLAYER || computerPlayer != currentPlayer || gameOverAnnounced || moveHandler != null) {
            return;
        }

        computerMoves.schedule(rows, columns, horizontalEdges, verticalEdges,
                computerDifficulty.skill(), random,
                () -> computerPlayer != NO_PLAYER && computerPlayer == currentPlayer && !gameOverAnnounced,
                move -> applyMove(move.horizontal, move.rowOrLine, move.columnOrLine));
    }

    private void stopComputerMoveTimer() {
        computerMoves.stop();
    }

    public enum ComputerDifficulty {
        EASY(Messages.DIFFICULTY_EASY, 0.22),
        MEDIUM(Messages.DIFFICULTY_MEDIUM, 0.61),
        HARD(Messages.DIFFICULTY_HARD, 0.94);

        private final String label;
        private final double skill;

        ComputerDifficulty(String label, double skill) {
            this.label = label;
            this.skill = skill;
        }

        double skill() {
            return skill;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public interface MoveHandler {
        void moveRequested(boolean horizontal, int rowOrLine, int columnOrLine);
    }

    public interface GameOverHandler {
        void gameOver(GameResult result);
    }

    public interface RestartHandler {
        void restartRequested();
    }

    public interface ClockTickHandler {
        void clockTick();
    }
}
