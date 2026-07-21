package cz.humblej.squares.ui;

final class BoardEdgeLocator {
    private BoardEdgeLocator() {
    }

    static BoardEdge find(int mouseX, int mouseY, int gridLeft, int gridTop,
                          int rows, int columns, int cellSize, int tolerance) {
        int gridX = mouseX - gridLeft;
        int gridY = mouseY - gridTop;
        int gridWidth = columns * cellSize;
        int gridHeight = rows * cellSize;
        if (gridX < 0 || gridX > gridWidth || gridY < 0 || gridY > gridHeight) {
            return null;
        }

        int nearestColumnLine = Math.round((float) gridX / cellSize);
        int nearestRowLine = Math.round((float) gridY / cellSize);
        int distanceToVertical = Math.abs(gridX - nearestColumnLine * cellSize);
        int distanceToHorizontal = Math.abs(gridY - nearestRowLine * cellSize);
        if (distanceToVertical > tolerance && distanceToHorizontal > tolerance) {
            return null;
        }

        if (distanceToVertical <= distanceToHorizontal) {
            if (nearestColumnLine <= 0 || nearestColumnLine >= columns) {
                return null;
            }
            return new BoardEdge(false, Math.min(gridY / cellSize, rows - 1), nearestColumnLine);
        }

        if (nearestRowLine <= 0 || nearestRowLine >= rows) {
            return null;
        }
        return new BoardEdge(true, nearestRowLine, Math.min(gridX / cellSize, columns - 1));
    }
}
