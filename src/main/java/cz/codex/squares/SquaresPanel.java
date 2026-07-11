package cz.codex.squares;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SquaresPanel extends JPanel {
    private static final int CELL_SIZE = 48;
    private static final int PADDING = 24;
    private static final int STATUS_HEIGHT = 66;
    private static final int INFO_HEIGHT = 72;
    private static final int SCORE_BOX_WIDTH = 80;
    private static final int SCORE_BOX_HEIGHT = 34;
    private static final int RESTART_BUTTON_WIDTH = 76;
    private static final int RESTART_BUTTON_HEIGHT = 30;
    private static final int CLICK_TOLERANCE = 8;

    static final int NO_PLAYER = 0;
    static final int RED_PLAYER = 1;
    static final int BLUE_PLAYER = 2;
    private static final int NEUTRAL_EDGE = 3;

    private static final Color BACKGROUND = new Color(248, 249, 250);
    private static final Color GRID_LINE = new Color(198, 204, 212);
    private static final Color CELL_FILL = new Color(255, 255, 255);
    private static final Color RED_FILL = new Color(255, 232, 228);
    private static final Color BLUE_FILL = new Color(226, 239, 255);
    private static final Color OUTER_BORDER = new Color(16, 18, 20);
    private static final Color RED_EDGE = new Color(228, 80, 58);
    private static final Color BLUE_EDGE = new Color(52, 116, 224);
    private static final Color ACTIVE_SCORE_BORDER = new Color(16, 18, 20);
    private static final Color SCORE_TEXT = new Color(255, 255, 255);
    private static final Color INFO_TEXT = new Color(96, 104, 114);

    private int rows;
    private int columns;
    private int[][] horizontalEdges;
    private int[][] verticalEdges;
    private int[][] completedCells;
    private final JButton restartButton;
    private EdgeHit hoveredEdge;
    private MoveHandler moveHandler;
    private GameOverHandler gameOverHandler;
    private RestartHandler restartHandler;
    private ClockTickHandler clockTickHandler;
    private int localPlayer = NO_PLAYER;
    private int currentPlayer = RED_PLAYER;
    private EdgeHit lastMove;
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

    SquaresPanel(int rows, int columns) {
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawStatus(g2);
            drawCells(g2);
            drawGhostEdge(g2);
            drawSelectedEdges(g2);
            drawOuterBorder(g2);
            drawInfo(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawStatus(Graphics2D g2) {
        int y = PADDING;
        int blueX = gridLeft() + columns * CELL_SIZE - SCORE_BOX_WIDTH;

        drawScoreBox(g2, PADDING, y, RED_PLAYER);
        drawScoreBox(g2, blueX, y, BLUE_PLAYER);
        drawStatusTimes(g2, PADDING, blueX);
    }

    private void drawScoreBox(Graphics2D g2, int x, int y, int player) {
        g2.setColor(edgeColor(player));
        g2.fillRect(x, y, SCORE_BOX_WIDTH, SCORE_BOX_HEIGHT);

        if (currentPlayer == player) {
            g2.setColor(ACTIVE_SCORE_BORDER);
            g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g2.drawRect(x + 2, y + 2, SCORE_BOX_WIDTH - 4, SCORE_BOX_HEIGHT - 4);
        }

        String score = Integer.toString(countCompletedCells(player));
        g2.setColor(SCORE_TEXT);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + (SCORE_BOX_WIDTH - metrics.stringWidth(score)) / 2;
        int textY = y + ((SCORE_BOX_HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(score, textX, textY);
    }

    private void drawStatusTimes(Graphics2D g2, int redX, int blueX) {
        int y = PADDING + SCORE_BOX_HEIGHT + 15;
        int centerX = gridLeft() + (columns * CELL_SIZE) / 2;

        g2.setColor(RED_EDGE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        drawCenteredText(g2, formatTime(redThinkingSeconds), redX + SCORE_BOX_WIDTH / 2, y);

        g2.setColor(BLUE_EDGE);
        drawCenteredText(g2, formatTime(blueThinkingSeconds), blueX + SCORE_BOX_WIDTH / 2, y);

        g2.setColor(OUTER_BORDER);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        drawCenteredText(g2, formatTime(totalSeconds), centerX, y);
    }

    private void drawCenteredText(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics metrics = g2.getFontMetrics();
        int textX = centerX - metrics.stringWidth(text) / 2;
        g2.drawString(text, textX, baselineY);
    }

    private void drawInfo(Graphics2D g2) {
        if (networkInfo.trim().isEmpty()) {
            return;
        }

        g2.setColor(INFO_TEXT);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        String[] lines = networkInfo.split("\\n");
        int textX = PADDING;
        FontMetrics metrics = g2.getFontMetrics();
        int blockHeight = lines.length * metrics.getHeight();
        int infoTop = gridTop() + rows * CELL_SIZE;
        int textY = infoTop + ((INFO_HEIGHT - blockHeight) / 2) + metrics.getAscent();

        for (String line : lines) {
            g2.drawString(line, textX, textY);
            textY += metrics.getHeight();
        }
    }

    private void drawCells(Graphics2D g2) {
        g2.setStroke(new BasicStroke(1.0f));

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = gridLeft() + column * CELL_SIZE;
                int y = gridTop() + row * CELL_SIZE;

                g2.setColor(cellColor(completedCells[row][column]));
                g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                g2.setColor(GRID_LINE);
                g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    private void drawSelectedEdges(Graphics2D g2) {
        g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int row = 1; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int owner = horizontalEdges[row][column];

                if (owner != NO_PLAYER) {
                    g2.setColor(isLastMove(true, row, column) ? edgeColor(owner) : OUTER_BORDER);
                    drawHorizontalEdge(g2, row, column);
                }
            }
        }

        for (int row = 0; row < rows; row++) {
            for (int column = 1; column < columns; column++) {
                int owner = verticalEdges[row][column];

                if (owner != NO_PLAYER) {
                    g2.setColor(isLastMove(false, row, column) ? edgeColor(owner) : OUTER_BORDER);
                    drawVerticalEdge(g2, row, column);
                }
            }
        }
    }

    private void drawOuterBorder(Graphics2D g2) {
        int size = CELL_SIZE * columns;

        g2.setColor(OUTER_BORDER);
        g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g2.drawRect(gridLeft(), gridTop(), size, size);
    }

    private void drawGhostEdge(Graphics2D g2) {
        if (hoveredEdge == null || isSelected(hoveredEdge)) {
            return;
        }

        if (localPlayer != NO_PLAYER && currentPlayer != localPlayer) {
            return;
        }

        g2.setColor(ghostColor(currentPlayer));
        g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (hoveredEdge.horizontal) {
            drawHorizontalEdge(g2, hoveredEdge.rowOrLine, hoveredEdge.columnOrLine);
        } else {
            drawVerticalEdge(g2, hoveredEdge.rowOrLine, hoveredEdge.columnOrLine);
        }
    }

    private void drawHorizontalEdge(Graphics2D g2, int rowLine, int column) {
        int x1 = gridLeft() + column * CELL_SIZE;
        int x2 = x1 + CELL_SIZE;
        int y = gridTop() + rowLine * CELL_SIZE;
        g2.drawLine(x1, y, x2, y);
    }

    private void drawVerticalEdge(Graphics2D g2, int row, int columnLine) {
        int x = gridLeft() + columnLine * CELL_SIZE;
        int y1 = gridTop() + row * CELL_SIZE;
        int y2 = y1 + CELL_SIZE;
        g2.drawLine(x, y1, x, y2);
    }

    private void selectEdgeAt(int mouseX, int mouseY) {
        EdgeHit edge = findEdgeAt(mouseX, mouseY);

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

    boolean applyMove(boolean horizontal, int rowOrLine, int columnOrLine) {
        if (gameOverAnnounced) {
            return false;
        }

        EdgeHit edge = new EdgeHit(horizontal, rowOrLine, columnOrLine);

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
        return true;
    }

    void setMoveHandler(MoveHandler moveHandler) {
        this.moveHandler = moveHandler;
    }

    void setGameOverHandler(GameOverHandler gameOverHandler) {
        this.gameOverHandler = gameOverHandler;
    }

    void setRestartHandler(RestartHandler restartHandler) {
        this.restartHandler = restartHandler;
    }

    void setClockTickHandler(ClockTickHandler clockTickHandler) {
        this.clockTickHandler = clockTickHandler;
    }

    void setLocalPlayer(int localPlayer) {
        this.localPlayer = localPlayer;
    }

    void setNetworkInfo(String networkInfo) {
        this.networkInfo = networkInfo == null ? "" : networkInfo;
        repaint();
    }

    void setClockEnabled(boolean clockEnabled) {
        this.clockEnabled = clockEnabled;
    }

    void resetClock() {
        initializeTimes();
        repaint();
    }

    void setClockPausedByDialog(boolean clockPausedByDialog) {
        this.clockPausedByDialog = clockPausedByDialog;
    }

    void setWindowActive(boolean windowActive) {
        this.windowActive = windowActive;
    }

    void setThinkingTimeLimitSeconds(int thinkingTimeLimitSeconds) {
        this.thinkingTimeLimitSeconds = Math.max(0, thinkingTimeLimitSeconds);
        initializeTimes();
        repaint();
    }

    void setRandomInitialEdgesEnabled(boolean randomInitialEdgesEnabled) {
        this.randomInitialEdgesEnabled = randomInitialEdgesEnabled;
    }

    boolean randomInitialEdgesEnabled() {
        return randomInitialEdgesEnabled;
    }

    String networkInfo() {
        return networkInfo;
    }

    void resetGame() {
        clear(horizontalEdges);
        clear(verticalEdges);
        clear(completedCells);
        hoveredEdge = null;
        lastMove = null;
        initializeTimes();
        currentPlayer = RED_PLAYER;
        gameOverAnnounced = false;
        generateRandomInitialEdges();
        repaint();
        SoundPlayer.gameStart();
    }

    void resizeBoard(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.horizontalEdges = new int[rows + 1][columns];
        this.verticalEdges = new int[rows][columns + 1];
        this.completedCells = new int[rows][columns];
        hoveredEdge = null;
        lastMove = null;
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
    }

    int getCurrentPlayer() {
        return currentPlayer;
    }

    int boardRows() {
        return rows;
    }

    int boardColumns() {
        return columns;
    }

    int thinkingTimeLimitSeconds() {
        return thinkingTimeLimitSeconds;
    }

    String encodeState() {
        return currentPlayer + "|" + encode(horizontalEdges) + "|" + encode(verticalEdges) + "|" + encode(completedCells)
                + "|" + encodeLastMove() + "|" + encodeTimes();
    }

    void applyEncodedState(String encodedState) {
        String[] parts = encodedState.split("\\|", -1);

        if (parts.length != 4 && parts.length != 5 && parts.length != 6) {
            return;
        }

        int edgeCountBefore = countSelectedEdges();
        int completedCountBefore = countCompletedCells(RED_PLAYER) + countCompletedCells(BLUE_PLAYER);
        currentPlayer = Integer.parseInt(parts[0]);
        decode(parts[1], horizontalEdges);
        decode(parts[2], verticalEdges);
        decode(parts[3], completedCells);
        lastMove = parts.length == 5 ? decodeLastMove(parts[4]) : null;
        if (parts.length == 6) {
            lastMove = decodeLastMove(parts[4]);
            decodeTimes(parts[5]);
        }
        playStateChangeSound(edgeCountBefore, completedCountBefore);
        repaint();
        checkGameOver();
    }

    private String encode(int[][] values) {
        StringBuilder builder = new StringBuilder(values.length * values[0].length);

        for (int[] row : values) {
            for (int value : row) {
                builder.append(value);
            }
        }

        return builder.toString();
    }

    private void decode(String encoded, int[][] target) {
        int index = 0;

        for (int row = 0; row < target.length; row++) {
            for (int column = 0; column < target[row].length; column++) {
                if (index < encoded.length()) {
                    target[row][column] = Character.digit(encoded.charAt(index), 10);
                }
                index++;
            }
        }
    }

    private void clear(int[][] target) {
        for (int row = 0; row < target.length; row++) {
            for (int column = 0; column < target[row].length; column++) {
                target[row][column] = NO_PLAYER;
            }
        }
    }

    private void generateRandomInitialEdges() {
        if (!randomInitialEdgesEnabled) {
            return;
        }

        List<EdgeHit> candidates = new ArrayList<>();

        for (int row = 1; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                candidates.add(new EdgeHit(true, row, column));
            }
        }

        for (int row = 0; row < rows; row++) {
            for (int column = 1; column < columns; column++) {
                candidates.add(new EdgeHit(false, row, column));
            }
        }

        Collections.shuffle(candidates);

        int targetCount = Math.min(candidates.size(), Math.max(1, rows * columns / 3));
        int generatedCount = 0;

        for (EdgeHit edge : candidates) {
            if (generatedCount >= targetCount) {
                return;
            }

            if (canAddRandomInitialEdge(edge)) {
                setEdgeOwner(edge, NEUTRAL_EDGE);
                generatedCount++;
            }
        }
    }

    private boolean canAddRandomInitialEdge(EdgeHit edge) {
        setEdgeOwner(edge, NEUTRAL_EDGE);
        boolean createsAlmostCompletedCell = hasAlmostCompletedCellAround(edge);
        setEdgeOwner(edge, NO_PLAYER);
        return !createsAlmostCompletedCell;
    }

    private boolean hasAlmostCompletedCellAround(EdgeHit edge) {
        if (edge.horizontal) {
            return hasAtLeastThreeEdgesIfValid(edge.rowOrLine - 1, edge.columnOrLine)
                    || hasAtLeastThreeEdgesIfValid(edge.rowOrLine, edge.columnOrLine);
        }

        return hasAtLeastThreeEdgesIfValid(edge.rowOrLine, edge.columnOrLine - 1)
                || hasAtLeastThreeEdgesIfValid(edge.rowOrLine, edge.columnOrLine);
    }

    private boolean hasAtLeastThreeEdgesIfValid(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < columns && countCellEdges(row, column) >= 3;
    }

    private int countCellEdges(int row, int column) {
        int count = 0;

        if (hasHorizontalEdge(row, column)) {
            count++;
        }

        if (hasHorizontalEdge(row + 1, column)) {
            count++;
        }

        if (hasVerticalEdge(row, column)) {
            count++;
        }

        if (hasVerticalEdge(row, column + 1)) {
            count++;
        }

        return count;
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
        SoundPlayer.gameOver();

        if (gameOverHandler != null) {
            gameOverHandler.gameOver(currentPlayer == RED_PLAYER
                    ? Messages.redLostOnTime()
                    : Messages.blueLostOnTime());
        }
    }

    private String encodeLastMove() {
        if (lastMove == null) {
            return "N";
        }

        return (lastMove.horizontal ? "H" : "V") + "," + lastMove.rowOrLine + "," + lastMove.columnOrLine;
    }

    private EdgeHit decodeLastMove(String encoded) {
        if (encoded == null || "N".equals(encoded)) {
            return null;
        }

        String[] parts = encoded.split(",");

        if (parts.length != 3) {
            return null;
        }

        boolean horizontal = "H".equals(parts[0]);
        int rowOrLine = Integer.parseInt(parts[1]);
        int columnOrLine = Integer.parseInt(parts[2]);
        return new EdgeHit(horizontal, rowOrLine, columnOrLine);
    }

    private String encodeTimes() {
        return totalSeconds + "," + redThinkingSeconds + "," + blueThinkingSeconds + "," + thinkingTimeLimitSeconds;
    }

    private void decodeTimes(String encoded) {
        String[] parts = encoded.split(",");

        if (parts.length != 3 && parts.length != 4) {
            return;
        }

        totalSeconds = Integer.parseInt(parts[0]);
        redThinkingSeconds = Integer.parseInt(parts[1]);
        blueThinkingSeconds = Integer.parseInt(parts[2]);

        if (parts.length == 4) {
            thinkingTimeLimitSeconds = Integer.parseInt(parts[3]);
        }
    }

    private EdgeHit findEdgeAt(int mouseX, int mouseY) {
        int gridX = mouseX - gridLeft();
        int gridY = mouseY - gridTop();
        int gridWidth = columns * CELL_SIZE;
        int gridHeight = rows * CELL_SIZE;

        if (gridX < 0 || gridX > gridWidth || gridY < 0 || gridY > gridHeight) {
            return null;
        }

        int nearestColumnLine = Math.round((float) gridX / CELL_SIZE);
        int nearestRowLine = Math.round((float) gridY / CELL_SIZE);
        int distanceToVertical = Math.abs(gridX - nearestColumnLine * CELL_SIZE);
        int distanceToHorizontal = Math.abs(gridY - nearestRowLine * CELL_SIZE);

        if (distanceToVertical > CLICK_TOLERANCE && distanceToHorizontal > CLICK_TOLERANCE) {
            return null;
        }

        if (distanceToVertical <= distanceToHorizontal) {
            if (nearestColumnLine <= 0 || nearestColumnLine >= columns) {
                return null;
            }

            int row = Math.min(gridY / CELL_SIZE, rows - 1);
            return new EdgeHit(false, row, nearestColumnLine);
        }

        if (nearestRowLine <= 0 || nearestRowLine >= rows) {
            return null;
        }

        int column = Math.min(gridX / CELL_SIZE, columns - 1);
        return new EdgeHit(true, nearestRowLine, column);
    }

    private int claimCompletedCellsAround(EdgeHit edge) {
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

    private void setHoveredEdge(EdgeHit edge) {
        if (EdgeHit.same(hoveredEdge, edge)) {
            return;
        }

        hoveredEdge = edge;
        setCursor(edge == null || isSelected(edge)
                ? Cursor.getDefaultCursor()
                : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        repaint();
    }

    private boolean isSelected(EdgeHit edge) {
        if (edge.horizontal) {
            return horizontalEdges[edge.rowOrLine][edge.columnOrLine] != NO_PLAYER;
        }

        return verticalEdges[edge.rowOrLine][edge.columnOrLine] != NO_PLAYER;
    }

    private void setEdgeOwner(EdgeHit edge, int player) {
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
        SoundPlayer.gameOver();

        if (gameOverHandler != null) {
            gameOverHandler.gameOver(winnerMessage());
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

    private String winnerMessage() {
        int redScore = countCompletedCells(RED_PLAYER);
        int blueScore = countCompletedCells(BLUE_PLAYER);

        if (redScore > blueScore) {
            return Messages.redWins(redScore, blueScore);
        }

        if (blueScore > redScore) {
            return Messages.blueWins(blueScore, redScore);
        }

        return Messages.draw(redScore, blueScore);
    }

    private Color cellColor(int owner) {
        if (owner == RED_PLAYER) {
            return RED_FILL;
        }

        if (owner == BLUE_PLAYER) {
            return BLUE_FILL;
        }

        return CELL_FILL;
    }

    private Color edgeColor(int owner) {
        if (owner == RED_PLAYER) {
            return RED_EDGE;
        }

        if (owner == BLUE_PLAYER) {
            return BLUE_EDGE;
        }

        return OUTER_BORDER;
    }

    private Color ghostColor(int owner) {
        Color color = edgeColor(owner);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 110);
    }

    private boolean isLastMove(boolean horizontal, int rowOrLine, int columnOrLine) {
        return lastMove != null
                && lastMove.horizontal == horizontal
                && lastMove.rowOrLine == rowOrLine
                && lastMove.columnOrLine == columnOrLine;
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return twoDigits(minutes) + ":" + twoDigits(remainingSeconds);
    }

    private String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    private int gridLeft() {
        return PADDING;
    }

    private int gridTop() {
        return PADDING + STATUS_HEIGHT;
    }

    private static final class EdgeHit {
        private final boolean horizontal;
        private final int rowOrLine;
        private final int columnOrLine;

        private EdgeHit(boolean horizontal, int rowOrLine, int columnOrLine) {
            this.horizontal = horizontal;
            this.rowOrLine = rowOrLine;
            this.columnOrLine = columnOrLine;
        }

        private static boolean same(EdgeHit first, EdgeHit second) {
            if (first == null || second == null) {
                return first == second;
            }

            return first.horizontal == second.horizontal
                    && first.rowOrLine == second.rowOrLine
                    && first.columnOrLine == second.columnOrLine;
        }
    }

    interface MoveHandler {
        void moveRequested(boolean horizontal, int rowOrLine, int columnOrLine);
    }

    interface GameOverHandler {
        void gameOver(String message);
    }

    interface RestartHandler {
        void restartRequested();
    }

    interface ClockTickHandler {
        void clockTick();
    }
}
