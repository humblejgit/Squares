package cz.humblej.squares.ui;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.game.GameEngine;
import cz.humblej.squares.game.GameMove;
import cz.humblej.squares.game.GameParticipant;
import cz.humblej.squares.game.GameResultFactory;
import cz.humblej.squares.game.GameSnapshot;

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
import java.util.Random;

public final class SquaresPanel extends JPanel {
    static final int CELL_SIZE = 48;
    static final int PADDING = 24;
    static final int STATUS_HEIGHT = 66;
    static final int INFO_HEIGHT = 72;
    private static final int RESTART_BUTTON_WIDTH = 76;
    private static final int RESTART_BUTTON_HEIGHT = 30;
    private static final int CLICK_TOLERANCE = 8;

    public static final int NO_PLAYER = GameEngine.NO_PLAYER;
    public static final int RED_PLAYER = GameEngine.RED_PLAYER;
    public static final int BLUE_PLAYER = GameEngine.BLUE_PLAYER;

    private static final Color BACKGROUND = new Color(248, 249, 250);

    private final GameEngine engine;
    private final JButton restartButton;
    private BoardEdge hoveredEdge;
    private MoveHandler moveHandler;
    private GameOverHandler gameOverHandler;
    private RestartHandler restartHandler;
    private ClockTickHandler clockTickHandler;
    private int localPlayer = NO_PLAYER;
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
    private final Random random = new Random();
    private final ComputerMoveController computerMoves = new ComputerMoveController();

    public SquaresPanel(int rows, int columns) {
        this.engine = new GameEngine(rows, columns);

        int width = engine.columns() * CELL_SIZE + PADDING * 2;
        int height = engine.rows() * CELL_SIZE + PADDING * 2 + STATUS_HEIGHT + INFO_HEIGHT;
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

        int x = gridLeft() + ((engine.columns() * CELL_SIZE) - RESTART_BUTTON_WIDTH) / 2;
        int y = PADDING + 2;
        restartButton.setBounds(x, y, RESTART_BUTTON_WIDTH, RESTART_BUTTON_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            GameSnapshot snapshot = engine.snapshot();
            SquaresPanelPainter.paint(g2, new SquaresPanelPainter.State(
                    snapshot.rows(), snapshot.columns(), snapshot.horizontalEdges(),
                    snapshot.verticalEdges(), snapshot.completedCells(), hoveredEdge,
                    toBoardEdge(snapshot.lastMove()), localPlayer, snapshot.currentPlayer(),
                    snapshot.totalSeconds(), snapshot.redThinkingSeconds(),
                    snapshot.blueThinkingSeconds(), networkInfo));
        } finally {
            g2.dispose();
        }
    }

    private void selectEdgeAt(int mouseX, int mouseY) {
        BoardEdge edge = findEdgeAt(mouseX, mouseY);

        if (edge == null || isSelected(edge)) {
            return;
        }

        if (localPlayer != NO_PLAYER && engine.currentPlayer() != localPlayer) {
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

        GameEngine.MoveResult result = engine.applyMove(new GameMove(horizontal, rowOrLine, columnOrLine));
        if (!result.applied()) {
            return false;
        }

        if (result.completedCells() == 0) {
            SoundPlayer.edge();
        } else {
            SoundPlayer.square();
        }

        repaint();
        if (result.gameOver()) {
            announceGameOver(GameResult.FinishReason.BOARD_FULL, NO_PLAYER);
        }
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
        engine.resetClock();
        repaint();
    }

    public void setClockPausedByDialog(boolean clockPausedByDialog) {
        this.clockPausedByDialog = clockPausedByDialog;
    }

    public void setWindowActive(boolean windowActive) {
        this.windowActive = windowActive;
    }

    public void setThinkingTimeLimitSeconds(int thinkingTimeLimitSeconds) {
        engine.setThinkingTimeLimitSeconds(thinkingTimeLimitSeconds);
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
        hoveredEdge = null;
        gameOverAnnounced = false;
        engine.reset(randomInitialEdgesEnabled, random);
        repaint();
        SoundPlayer.gameStart();
        scheduleComputerMoveIfNeeded();
    }

    public void resizeBoard(int rows, int columns) {
        stopComputerMoveTimer();
        hoveredEdge = null;
        gameOverAnnounced = false;
        engine.resize(rows, columns, randomInitialEdgesEnabled, random);

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
        return engine.currentPlayer();
    }

    public int boardRows() {
        return engine.rows();
    }

    public int boardColumns() {
        return engine.columns();
    }

    public int thinkingTimeLimitSeconds() {
        return engine.thinkingTimeLimitSeconds();
    }

    public String encodeState() {
        GameSnapshot snapshot = engine.snapshot();
        return snapshot.currentPlayer() + "|" + SquaresPanelStateCodec.encodeMatrix(snapshot.horizontalEdges())
                + "|" + SquaresPanelStateCodec.encodeMatrix(snapshot.verticalEdges())
                + "|" + SquaresPanelStateCodec.encodeMatrix(snapshot.completedCells())
                + "|" + SquaresPanelStateCodec.encodeEdge(toBoardEdge(snapshot.lastMove()))
                + "|" + SquaresPanelStateCodec.encodeTimes(snapshot.totalSeconds(),
                        snapshot.redThinkingSeconds(), snapshot.blueThinkingSeconds(),
                        snapshot.thinkingTimeLimitSeconds());
    }

    public void applyEncodedState(String encodedState) {
        String[] parts = encodedState.split("\\|", -1);

        if (parts.length != 4 && parts.length != 5 && parts.length != 6) {
            return;
        }

        int edgeCountBefore = engine.selectedEdgeCount();
        int completedCountBefore = engine.score(RED_PLAYER) + engine.score(BLUE_PLAYER);
        int[][] horizontalEdges = new int[engine.rows() + 1][engine.columns()];
        int[][] verticalEdges = new int[engine.rows()][engine.columns() + 1];
        int[][] completedCells = new int[engine.rows()][engine.columns()];
        SquaresPanelStateCodec.decodeMatrix(parts[1], horizontalEdges);
        SquaresPanelStateCodec.decodeMatrix(parts[2], verticalEdges);
        SquaresPanelStateCodec.decodeMatrix(parts[3], completedCells);
        BoardEdge lastMove = parts.length >= 5 ? SquaresPanelStateCodec.decodeEdge(parts[4]) : null;
        GameSnapshot previous = engine.snapshot();
        int totalSeconds = previous.totalSeconds();
        int redThinkingSeconds = previous.redThinkingSeconds();
        int blueThinkingSeconds = previous.blueThinkingSeconds();
        int thinkingTimeLimitSeconds = previous.thinkingTimeLimitSeconds();
        if (parts.length == 6) {
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
        engine.restore(new GameSnapshot(engine.rows(), engine.columns(), horizontalEdges, verticalEdges,
                completedCells, Integer.parseInt(parts[0]), toGameMove(lastMove), totalSeconds,
                redThinkingSeconds, blueThinkingSeconds, thinkingTimeLimitSeconds));
        playStateChangeSound(edgeCountBefore, completedCountBefore);
        repaint();
        if (engine.gameOver()) {
            announceGameOver(GameResult.FinishReason.BOARD_FULL, NO_PLAYER);
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

        GameEngine.TickResult result = engine.tickClock();

        repaint();

        if (clockTickHandler != null) {
            clockTickHandler.clockTick();
        }

        if (result.timeExpired()) {
            announceGameOver(GameResult.FinishReason.TIME_LIMIT, result.timedOutPlayer());
        }
    }

    private void announceGameOver(GameResult.FinishReason finishReason, int timedOutPlayer) {
        if (gameOverAnnounced) {
            return;
        }

        gameOverAnnounced = true;
        stopComputerMoveTimer();
        SoundPlayer.gameOver();

        if (gameOverHandler != null) {
            gameOverHandler.gameOver(createGameResult(finishReason, timedOutPlayer));
        }
    }

    private BoardEdge findEdgeAt(int mouseX, int mouseY) {
        return BoardEdgeLocator.find(mouseX, mouseY, gridLeft(), gridTop(),
                engine.rows(), engine.columns(), CELL_SIZE, CLICK_TOLERANCE);
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
        return engine.isSelected(toGameMove(edge));
    }

    private void playStateChangeSound(int edgeCountBefore, int completedCountBefore) {
        int edgeCountAfter = engine.selectedEdgeCount();
        int completedCountAfter = engine.score(RED_PLAYER) + engine.score(BLUE_PLAYER);

        if (completedCountAfter > completedCountBefore) {
            SoundPlayer.square();
        } else if (edgeCountAfter > edgeCountBefore) {
            SoundPlayer.edge();
        }
    }

    private GameResult createGameResult(GameResult.FinishReason finishReason, int timedOutPlayer) {
        return GameResultFactory.create(engine, gameMode(), finishReason, timedOutPlayer,
                randomInitialEdgesEnabled, gameResultCpuDifficulty(), participant(RED_PLAYER),
                participant(BLUE_PLAYER));
    }

    private GameParticipant participant(int player) {
        PlayerProfile profile = player == RED_PLAYER ? redProfile : blueProfile;
        if (computerPlayer == player) {
            return GameParticipant.computer(Messages.PLAYER_CPU);
        }
        if (profile != null) {
            return GameParticipant.profile(profile);
        }
        return GameParticipant.guest(player == RED_PLAYER ? redPlayerDisplayName() : bluePlayerDisplayName());
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

    private int gridLeft() {
        return PADDING;
    }

    private int gridTop() {
        return PADDING + STATUS_HEIGHT;
    }

    private void scheduleComputerMoveIfNeeded() {
        if (computerPlayer == NO_PLAYER || computerPlayer != engine.currentPlayer()
                || gameOverAnnounced || moveHandler != null) {
            return;
        }

        GameSnapshot snapshot = engine.snapshot();
        computerMoves.schedule(snapshot.rows(), snapshot.columns(), snapshot.horizontalEdges(),
                snapshot.verticalEdges(),
                computerDifficulty.skill(), random,
                () -> computerPlayer != NO_PLAYER && computerPlayer == engine.currentPlayer()
                        && !gameOverAnnounced,
                move -> applyMove(move.horizontal(), move.rowOrLine(), move.columnOrLine()));
    }

    private static GameMove toGameMove(BoardEdge edge) {
        return edge == null ? null : new GameMove(edge.horizontal, edge.rowOrLine, edge.columnOrLine);
    }

    private static BoardEdge toBoardEdge(GameMove move) {
        return move == null ? null : new BoardEdge(move.horizontal(), move.rowOrLine(), move.columnOrLine());
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
