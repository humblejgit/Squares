package cz.humblej.squares.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class SquaresPanelPainter {
    private static final int SCORE_BOX_WIDTH = 80;
    private static final int SCORE_BOX_HEIGHT = 34;

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

    private SquaresPanelPainter() {
    }

    static void paint(Graphics2D graphics, State state) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawStatus(graphics, state);
        drawCells(graphics, state);
        drawGhostEdge(graphics, state);
        drawSelectedEdges(graphics, state);
        drawOuterBorder(graphics, state);
        drawInfo(graphics, state);
    }

    private static void drawStatus(Graphics2D graphics, State state) {
        int y = SquaresPanel.PADDING;
        int blueX = gridLeft() + state.columns * SquaresPanel.CELL_SIZE - SCORE_BOX_WIDTH;

        drawScoreBox(graphics, state, SquaresPanel.PADDING, y, SquaresPanel.RED_PLAYER);
        drawScoreBox(graphics, state, blueX, y, SquaresPanel.BLUE_PLAYER);
        drawStatusTimes(graphics, state, SquaresPanel.PADDING, blueX);
    }

    private static void drawScoreBox(Graphics2D graphics, State state, int x, int y, int player) {
        graphics.setColor(edgeColor(player));
        graphics.fillRect(x, y, SCORE_BOX_WIDTH, SCORE_BOX_HEIGHT);

        if (state.currentPlayer == player) {
            graphics.setColor(ACTIVE_SCORE_BORDER);
            graphics.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            graphics.drawRect(x + 2, y + 2, SCORE_BOX_WIDTH - 4, SCORE_BOX_HEIGHT - 4);
        }

        String score = Integer.toString(countCompletedCells(state, player));
        graphics.setColor(SCORE_TEXT);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

        FontMetrics metrics = graphics.getFontMetrics();
        int textX = x + (SCORE_BOX_WIDTH - metrics.stringWidth(score)) / 2;
        int textY = y + ((SCORE_BOX_HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent();
        graphics.drawString(score, textX, textY);
    }

    private static void drawStatusTimes(Graphics2D graphics, State state, int redX, int blueX) {
        int y = SquaresPanel.PADDING + SCORE_BOX_HEIGHT + 15;
        int centerX = gridLeft() + (state.columns * SquaresPanel.CELL_SIZE) / 2;

        graphics.setColor(RED_EDGE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        drawCenteredText(graphics, formatTime(state.redThinkingSeconds), redX + SCORE_BOX_WIDTH / 2, y);

        graphics.setColor(BLUE_EDGE);
        drawCenteredText(graphics, formatTime(state.blueThinkingSeconds), blueX + SCORE_BOX_WIDTH / 2, y);

        graphics.setColor(OUTER_BORDER);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        drawCenteredText(graphics, formatTime(state.totalSeconds), centerX, y);
    }

    private static void drawCenteredText(Graphics2D graphics, String text, int centerX, int baselineY) {
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
    }

    private static void drawInfo(Graphics2D graphics, State state) {
        if (state.networkInfo.trim().isEmpty()) {
            return;
        }

        graphics.setColor(INFO_TEXT);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        String[] lines = state.networkInfo.split("\\n");
        int textX = SquaresPanel.PADDING;
        FontMetrics metrics = graphics.getFontMetrics();
        int blockHeight = lines.length * metrics.getHeight();
        int infoTop = gridTop() + state.rows * SquaresPanel.CELL_SIZE;
        int textY = infoTop + ((SquaresPanel.INFO_HEIGHT - blockHeight) / 2) + metrics.getAscent();

        for (String line : lines) {
            graphics.drawString(line, textX, textY);
            textY += metrics.getHeight();
        }
    }

    private static void drawCells(Graphics2D graphics, State state) {
        graphics.setStroke(new BasicStroke(1.0f));

        for (int row = 0; row < state.rows; row++) {
            for (int column = 0; column < state.columns; column++) {
                int x = gridLeft() + column * SquaresPanel.CELL_SIZE;
                int y = gridTop() + row * SquaresPanel.CELL_SIZE;

                graphics.setColor(cellColor(state.completedCells[row][column]));
                graphics.fillRect(x, y, SquaresPanel.CELL_SIZE, SquaresPanel.CELL_SIZE);

                graphics.setColor(GRID_LINE);
                graphics.drawRect(x, y, SquaresPanel.CELL_SIZE, SquaresPanel.CELL_SIZE);
            }
        }
    }

    private static void drawSelectedEdges(Graphics2D graphics, State state) {
        graphics.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int row = 1; row < state.rows; row++) {
            for (int column = 0; column < state.columns; column++) {
                int owner = state.horizontalEdges[row][column];
                if (owner != SquaresPanel.NO_PLAYER) {
                    graphics.setColor(isLastMove(state, true, row, column) ? edgeColor(owner) : OUTER_BORDER);
                    drawHorizontalEdge(graphics, row, column);
                }
            }
        }

        for (int row = 0; row < state.rows; row++) {
            for (int column = 1; column < state.columns; column++) {
                int owner = state.verticalEdges[row][column];
                if (owner != SquaresPanel.NO_PLAYER) {
                    graphics.setColor(isLastMove(state, false, row, column) ? edgeColor(owner) : OUTER_BORDER);
                    drawVerticalEdge(graphics, row, column);
                }
            }
        }
    }

    private static void drawOuterBorder(Graphics2D graphics, State state) {
        graphics.setColor(OUTER_BORDER);
        graphics.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        graphics.drawRect(gridLeft(), gridTop(), SquaresPanel.CELL_SIZE * state.columns,
                SquaresPanel.CELL_SIZE * state.columns);
    }

    private static void drawGhostEdge(Graphics2D graphics, State state) {
        if (state.hoveredEdge == null || isSelected(state, state.hoveredEdge)) {
            return;
        }
        if (state.localPlayer != SquaresPanel.NO_PLAYER && state.currentPlayer != state.localPlayer) {
            return;
        }

        graphics.setColor(ghostColor(state.currentPlayer));
        graphics.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (state.hoveredEdge.horizontal) {
            drawHorizontalEdge(graphics, state.hoveredEdge.rowOrLine, state.hoveredEdge.columnOrLine);
        } else {
            drawVerticalEdge(graphics, state.hoveredEdge.rowOrLine, state.hoveredEdge.columnOrLine);
        }
    }

    private static void drawHorizontalEdge(Graphics2D graphics, int rowLine, int column) {
        int x1 = gridLeft() + column * SquaresPanel.CELL_SIZE;
        int y = gridTop() + rowLine * SquaresPanel.CELL_SIZE;
        graphics.drawLine(x1, y, x1 + SquaresPanel.CELL_SIZE, y);
    }

    private static void drawVerticalEdge(Graphics2D graphics, int row, int columnLine) {
        int x = gridLeft() + columnLine * SquaresPanel.CELL_SIZE;
        int y1 = gridTop() + row * SquaresPanel.CELL_SIZE;
        graphics.drawLine(x, y1, x, y1 + SquaresPanel.CELL_SIZE);
    }

    private static boolean isSelected(State state, BoardEdge edge) {
        return edge.horizontal
                ? state.horizontalEdges[edge.rowOrLine][edge.columnOrLine] != SquaresPanel.NO_PLAYER
                : state.verticalEdges[edge.rowOrLine][edge.columnOrLine] != SquaresPanel.NO_PLAYER;
    }

    private static boolean isLastMove(State state, boolean horizontal, int rowOrLine, int columnOrLine) {
        return state.lastMove != null
                && state.lastMove.horizontal == horizontal
                && state.lastMove.rowOrLine == rowOrLine
                && state.lastMove.columnOrLine == columnOrLine;
    }

    private static int countCompletedCells(State state, int player) {
        int count = 0;
        for (int[] row : state.completedCells) {
            for (int owner : row) {
                if (owner == player) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Color cellColor(int owner) {
        if (owner == SquaresPanel.RED_PLAYER) {
            return RED_FILL;
        }
        return owner == SquaresPanel.BLUE_PLAYER ? BLUE_FILL : CELL_FILL;
    }

    private static Color edgeColor(int owner) {
        if (owner == SquaresPanel.RED_PLAYER) {
            return RED_EDGE;
        }
        return owner == SquaresPanel.BLUE_PLAYER ? BLUE_EDGE : OUTER_BORDER;
    }

    private static Color ghostColor(int owner) {
        Color color = edgeColor(owner);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 110);
    }

    private static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return twoDigits(minutes) + ":" + twoDigits(remainingSeconds);
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    private static int gridLeft() {
        return SquaresPanel.PADDING;
    }

    private static int gridTop() {
        return SquaresPanel.PADDING + SquaresPanel.STATUS_HEIGHT;
    }

    static final class State {
        final int rows;
        final int columns;
        final int[][] horizontalEdges;
        final int[][] verticalEdges;
        final int[][] completedCells;
        final BoardEdge hoveredEdge;
        final BoardEdge lastMove;
        final int localPlayer;
        final int currentPlayer;
        final int totalSeconds;
        final int redThinkingSeconds;
        final int blueThinkingSeconds;
        final String networkInfo;

        State(int rows, int columns, int[][] horizontalEdges, int[][] verticalEdges, int[][] completedCells,
              BoardEdge hoveredEdge, BoardEdge lastMove, int localPlayer, int currentPlayer,
              int totalSeconds, int redThinkingSeconds, int blueThinkingSeconds, String networkInfo) {
            this.rows = rows;
            this.columns = columns;
            this.horizontalEdges = horizontalEdges;
            this.verticalEdges = verticalEdges;
            this.completedCells = completedCells;
            this.hoveredEdge = hoveredEdge;
            this.lastMove = lastMove;
            this.localPlayer = localPlayer;
            this.currentPlayer = currentPlayer;
            this.totalSeconds = totalSeconds;
            this.redThinkingSeconds = redThinkingSeconds;
            this.blueThinkingSeconds = blueThinkingSeconds;
            this.networkInfo = networkInfo;
        }
    }
}
