package cz.humblej.squares.ui;

final class SquaresPanelStateCodec {
    private SquaresPanelStateCodec() {
    }

    static String encodeMatrix(int[][] values) {
        StringBuilder builder = new StringBuilder(values.length * values[0].length);
        for (int[] row : values) {
            for (int value : row) {
                builder.append(value);
            }
        }
        return builder.toString();
    }

    static void decodeMatrix(String encoded, int[][] target) {
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

    static String encodeEdge(BoardEdge edge) {
        if (edge == null) {
            return "N";
        }
        return (edge.horizontal ? "H" : "V") + "," + edge.rowOrLine + "," + edge.columnOrLine;
    }

    static BoardEdge decodeEdge(String encoded) {
        if (encoded == null || "N".equals(encoded)) {
            return null;
        }

        String[] parts = encoded.split(",");
        if (parts.length != 3) {
            return null;
        }

        return new BoardEdge("H".equals(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    static String encodeTimes(int totalSeconds, int redSeconds, int blueSeconds, int limitSeconds) {
        return totalSeconds + "," + redSeconds + "," + blueSeconds + "," + limitSeconds;
    }

    static int[] decodeTimes(String encoded) {
        String[] parts = encoded.split(",");
        if (parts.length != 3 && parts.length != 4) {
            return null;
        }

        int limit = parts.length == 4 ? Integer.parseInt(parts[3]) : -1;
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]), limit};
    }
}
