package cz.humblej.squares.app;

import cz.humblej.squares.ui.SquaresPanel;

final class GameOptions {
    private final int boardSize;
    private final int thinkingTimeSeconds;
    private final boolean randomEdges;
    private final SquaresPanel.ComputerDifficulty computerDifficulty;

    GameOptions(int boardSize, int thinkingTimeSeconds, boolean randomEdges,
                SquaresPanel.ComputerDifficulty computerDifficulty) {
        this.boardSize = boardSize;
        this.thinkingTimeSeconds = thinkingTimeSeconds;
        this.randomEdges = randomEdges;
        this.computerDifficulty = computerDifficulty;
    }

    int boardSize() {
        return boardSize;
    }

    int thinkingTimeSeconds() {
        return thinkingTimeSeconds;
    }

    boolean randomEdges() {
        return randomEdges;
    }

    SquaresPanel.ComputerDifficulty computerDifficulty() {
        return computerDifficulty;
    }
}
