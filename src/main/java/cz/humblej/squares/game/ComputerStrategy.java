package cz.humblej.squares.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Chooses CPU moves without depending on Swing. The board used by the game has
 * its outer border filled, so only internal edges need to be represented here.
 */
public final class ComputerStrategy {
    private static final int NO_PLAYER = 0;
    private static final int BOX_VALUE = 1000;
    private static final int INFINITY = 1_000_000_000;
    private static final long HARD_THINK_NANOS = 120_000_000L;
    private static final long HARD_NODE_LIMIT = 150_000L;

    private ComputerStrategy() {
    }

    public static Move chooseMove(int rows, int columns, int[][] horizontalEdges, int[][] verticalEdges,
                           double skill, Random random) {
        return chooseDecision(rows, columns, horizontalEdges, verticalEdges, skill, random).move;
    }

    public static Decision chooseDecision(int rows, int columns, int[][] horizontalEdges, int[][] verticalEdges,
                                   double skill, Random random) {
        Board board = new Board(rows, columns, horizontalEdges, verticalEdges);
        List<Candidate> candidates = evaluateCandidates(board, random);

        if (candidates.isEmpty()) {
            return new Decision(null, 0);
        }

        Move move;
        if (skill < 0.45) {
            move = chooseEasy(board, candidates, random);
        } else if (skill < 0.85) {
            move = chooseMedium(board, candidates, random);
        } else {
            int fallback = candidates.get(0).moveIndex;
            int best = new Search(board, random).chooseMove(fallback);
            move = board.move(best);
        }

        int moveIndex = board.indexOf(move);
        return new Decision(move, thinkingDelayMillis(board, moveIndex, skill, random));
    }

    private static Move chooseEasy(Board board, List<Candidate> candidates, Random random) {
        if (random.nextDouble() < 0.60) {
            return board.move(candidates.get(0).moveIndex);
        }

        int candidateCount = Math.min(candidates.size(), 10);
        return board.move(candidates.get(random.nextInt(candidateCount)).moveIndex);
    }

    private static Move chooseMedium(Board board, List<Candidate> candidates, Random random) {
        if (random.nextDouble() < 0.92) {
            return board.move(candidates.get(0).moveIndex);
        }

        int candidateCount = Math.min(candidates.size(), 3);
        return board.move(candidates.get(random.nextInt(candidateCount)).moveIndex);
    }

    private static List<Candidate> evaluateCandidates(Board board, Random random) {
        List<Candidate> candidates = new ArrayList<>();

        for (int moveIndex : board.legalMoves()) {
            int captured = board.captureCount(moveIndex);
            int risk = board.riskCount(moveIndex);
            int flexibility = board.flexibility(moveIndex);
            int forcedGain = captured > 0 || risk > 0 ? board.forcedCaptureGainAfter(moveIndex) : 0;
            long score;

            if (captured > 0) {
                score = 3_000_000L + forcedGain * 100_000L + captured * 5_000L + flexibility;
            } else if (risk == 0) {
                score = 1_000_000L + flexibility * 100L;
            } else {
                score = -forcedGain * 100_000L - risk * 1_000L + flexibility;
            }

            candidates.add(new Candidate(moveIndex, score));
        }

        Collections.shuffle(candidates, random);
        Collections.sort(candidates);
        return candidates;
    }

    private static int thinkingDelayMillis(Board board, int moveIndex, double skill, Random random) {
        int selectedCaptures = board.captureCount(moveIndex);
        int selectedRisk = board.riskCount(moveIndex);
        int captureOptions = 0;
        int safeOptions = 0;

        for (int candidate : board.legalMoves()) {
            int captures = board.captureCount(candidate);
            if (captures > 0) {
                captureOptions++;
            } else if (board.riskCount(candidate) == 0) {
                safeOptions++;
            }
        }

        int minimum;
        int maximum;
        if (selectedCaptures > 0) {
            int forcedGain = board.forcedCaptureGainAfter(moveIndex);
            minimum = forcedGain <= 2 && captureOptions <= 2 ? 300 : 450;
            maximum = forcedGain <= 2 && captureOptions <= 2 ? 700 : 1000;
        } else if (captureOptions > 0) {
            int forcedGain = selectedRisk > 0 ? board.forcedCaptureGainAfter(moveIndex) : 1;
            minimum = 1800 + Math.min(500, forcedGain * 100);
            maximum = 3200 + Math.min(1200, forcedGain * 180);
        } else if (selectedRisk == 0 && safeOptions <= 3) {
            minimum = 600;
            maximum = 1200;
        } else if (selectedRisk == 0) {
            minimum = 900;
            maximum = 1900 + Math.min(200, safeOptions * 5);
        } else {
            int forcedGain = board.forcedCaptureGainAfter(moveIndex);
            minimum = 1700 + Math.min(600, forcedGain * 100);
            maximum = 3000 + Math.min(1400, forcedGain * 180);
        }

        int randomized = minimum + random.nextInt(maximum - minimum + 1);
        double difficultyFactor = 0.86 + skill * 0.25;
        return Math.max(250, Math.min(5000, (int) Math.round(randomized * difficultyFactor)));
    }

    public static final class Decision {
        public final Move move;
        public final int thinkingDelayMillis;

        private Decision(Move move, int thinkingDelayMillis) {
            this.move = move;
            this.thinkingDelayMillis = thinkingDelayMillis;
        }
    }

    public static final class Move {
        public final boolean horizontal;
        public final int rowOrLine;
        public final int columnOrLine;

        private Move(boolean horizontal, int rowOrLine, int columnOrLine, int firstCell, int secondCell) {
            this.horizontal = horizontal;
            this.rowOrLine = rowOrLine;
            this.columnOrLine = columnOrLine;
            this.firstCell = firstCell;
            this.secondCell = secondCell;
        }

        private final int firstCell;
        private final int secondCell;
    }

    private static final class Candidate implements Comparable<Candidate> {
        private final int moveIndex;
        private final long score;

        private Candidate(int moveIndex, long score) {
            this.moveIndex = moveIndex;
            this.score = score;
        }

        @Override
        public int compareTo(Candidate other) {
            return Long.compare(other.score, score);
        }
    }

    private static final class Board {
        private final List<Move> moves = new ArrayList<>();
        private final boolean[] selected;
        private final int[] missingEdges;
        private final long[] zobrist;
        private int remainingMoves;
        private long hash;

        private Board(int rows, int columns, int[][] horizontalEdges, int[][] verticalEdges) {
            for (int rowLine = 1; rowLine < rows; rowLine++) {
                for (int column = 0; column < columns; column++) {
                    moves.add(new Move(true, rowLine, column,
                            cellIndex(rowLine - 1, column, columns),
                            cellIndex(rowLine, column, columns)));
                }
            }

            for (int row = 0; row < rows; row++) {
                for (int columnLine = 1; columnLine < columns; columnLine++) {
                    moves.add(new Move(false, row, columnLine,
                            cellIndex(row, columnLine - 1, columns),
                            cellIndex(row, columnLine, columns)));
                }
            }

            selected = new boolean[moves.size()];
            missingEdges = new int[rows * columns];
            zobrist = new long[moves.size()];

            for (int index = 0; index < moves.size(); index++) {
                Move move = moves.get(index);
                selected[index] = move.horizontal
                        ? horizontalEdges[move.rowOrLine][move.columnOrLine] != NO_PLAYER
                        : verticalEdges[move.rowOrLine][move.columnOrLine] != NO_PLAYER;
                zobrist[index] = mix64(index + 0x9E3779B97F4A7C15L);

                if (selected[index]) {
                    hash ^= zobrist[index];
                } else {
                    remainingMoves++;
                    missingEdges[move.firstCell]++;
                    missingEdges[move.secondCell]++;
                }
            }
        }

        private static int cellIndex(int row, int column, int columns) {
            return row * columns + column;
        }

        private static long mix64(long value) {
            value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
            value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
            return value ^ (value >>> 31);
        }

        private Move move(int moveIndex) {
            return moves.get(moveIndex);
        }

        private int indexOf(Move move) {
            return moves.indexOf(move);
        }

        private List<Integer> legalMoves() {
            List<Integer> legal = new ArrayList<>(remainingMoves);

            for (int index = 0; index < selected.length; index++) {
                if (!selected[index]) {
                    legal.add(index);
                }
            }

            return legal;
        }

        private int captureCount(int moveIndex) {
            Move move = moves.get(moveIndex);
            int captured = missingEdges[move.firstCell] == 1 ? 1 : 0;
            return captured + (missingEdges[move.secondCell] == 1 ? 1 : 0);
        }

        private int riskCount(int moveIndex) {
            Move move = moves.get(moveIndex);
            int risk = missingEdges[move.firstCell] == 2 ? 1 : 0;
            return risk + (missingEdges[move.secondCell] == 2 ? 1 : 0);
        }

        private int flexibility(int moveIndex) {
            Move move = moves.get(moveIndex);
            return missingEdges[move.firstCell] + missingEdges[move.secondCell];
        }

        private int apply(int moveIndex) {
            Move move = moves.get(moveIndex);
            selected[moveIndex] = true;
            remainingMoves--;
            hash ^= zobrist[moveIndex];
            missingEdges[move.firstCell]--;
            missingEdges[move.secondCell]--;

            int captured = missingEdges[move.firstCell] == 0 ? 1 : 0;
            return captured + (missingEdges[move.secondCell] == 0 ? 1 : 0);
        }

        private void undo(int moveIndex) {
            Move move = moves.get(moveIndex);
            missingEdges[move.firstCell]++;
            missingEdges[move.secondCell]++;
            hash ^= zobrist[moveIndex];
            remainingMoves++;
            selected[moveIndex] = false;
        }

        private int forcedCaptureGainAfter(int moveIndex) {
            List<Integer> appliedMoves = new ArrayList<>();
            int totalCaptured = apply(moveIndex);
            appliedMoves.add(moveIndex);

            while (true) {
                int bestMove = -1;
                int bestCaptured = 0;

                for (int candidate : legalMoves()) {
                    int captured = captureCount(candidate);
                    if (captured > bestCaptured) {
                        bestCaptured = captured;
                        bestMove = candidate;
                    }
                }

                if (bestMove < 0) {
                    break;
                }

                totalCaptured += apply(bestMove);
                appliedMoves.add(bestMove);
            }

            for (int index = appliedMoves.size() - 1; index >= 0; index--) {
                undo(appliedMoves.get(index));
            }

            return totalCaptured;
        }
    }

    private static final class Search {
        private final Board board;
        private final Random random;
        private final long deadline;
        private final Map<Long, Transposition> transpositions = new HashMap<>();
        private long nodes;

        private Search(Board board, Random random) {
            this.board = board;
            this.random = random;
            this.deadline = System.nanoTime() + HARD_THINK_NANOS;
        }

        private int chooseMove(int fallback) {
            int bestMove = fallback;
            int maximumDepth = maximumDepth();

            for (int depth = 1; depth <= maximumDepth; depth++) {
                try {
                    bestMove = searchRoot(depth);
                } catch (SearchTimeout timeout) {
                    break;
                }
            }

            return bestMove;
        }

        private int maximumDepth() {
            if (board.remainingMoves <= 22) {
                return board.remainingMoves;
            }
            if (board.remainingMoves <= 40) {
                return 9;
            }
            if (board.remainingMoves <= 70) {
                return 6;
            }
            return 3;
        }

        private int searchRoot(int depth) {
            List<Integer> moves = orderedMoves(true);
            int bestMove = moves.get(0);
            int bestValue = -INFINITY;

            for (int moveIndex : moves) {
                checkBudget();
                int captured = board.apply(moveIndex);
                int value;

                try {
                    if (captured > 0) {
                        value = captured * BOX_VALUE + negamax(depth, -INFINITY, INFINITY);
                    } else {
                        value = -negamax(depth - 1, -INFINITY, INFINITY);
                    }
                } finally {
                    board.undo(moveIndex);
                }

                if (value > bestValue) {
                    bestValue = value;
                    bestMove = moveIndex;
                }
            }

            return bestMove;
        }

        private int negamax(int depth, int alpha, int beta) {
            checkBudget();

            if (board.remainingMoves == 0) {
                return 0;
            }

            if (depth <= 0) {
                return evaluatePosition();
            }

            int originalAlpha = alpha;
            int originalBeta = beta;
            Transposition cached = transpositions.get(board.hash);

            if (cached != null && cached.depth >= depth) {
                if (cached.bound == Bound.EXACT) {
                    return cached.value;
                }
                if (cached.bound == Bound.LOWER) {
                    alpha = Math.max(alpha, cached.value);
                } else {
                    beta = Math.min(beta, cached.value);
                }
                if (alpha >= beta) {
                    return cached.value;
                }
            }

            int bestValue = -INFINITY;
            List<Integer> moves = limitedMoves(orderedMoves(false));

            for (int moveIndex : moves) {
                int captured = board.apply(moveIndex);
                int value;

                try {
                    if (captured > 0) {
                        int gain = captured * BOX_VALUE;
                        value = gain + negamax(depth, alpha - gain, beta - gain);
                    } else {
                        value = -negamax(depth - 1, -beta, -alpha);
                    }
                } finally {
                    board.undo(moveIndex);
                }

                bestValue = Math.max(bestValue, value);
                alpha = Math.max(alpha, value);
                if (alpha >= beta) {
                    break;
                }
            }

            Bound bound = Bound.EXACT;
            if (bestValue <= originalAlpha) {
                bound = Bound.UPPER;
            } else if (bestValue >= originalBeta) {
                bound = Bound.LOWER;
            }
            transpositions.put(board.hash, new Transposition(depth, bestValue, bound));
            return bestValue;
        }

        private int evaluatePosition() {
            List<Integer> moves = board.legalMoves();
            int bestForcedGain = 0;
            int safeMoves = 0;
            int smallestSacrifice = Integer.MAX_VALUE;

            for (int moveIndex : moves) {
                int captured = board.captureCount(moveIndex);
                int risk = board.riskCount(moveIndex);

                if (captured > 0) {
                    bestForcedGain = Math.max(bestForcedGain, board.forcedCaptureGainAfter(moveIndex));
                } else if (risk == 0) {
                    safeMoves++;
                } else if (bestForcedGain == 0) {
                    smallestSacrifice = Math.min(smallestSacrifice, board.forcedCaptureGainAfter(moveIndex));
                }
            }

            if (bestForcedGain > 0) {
                return bestForcedGain * BOX_VALUE;
            }
            if (safeMoves > 0) {
                return (safeMoves & 1) == 0 ? 4 : -4;
            }
            if (smallestSacrifice != Integer.MAX_VALUE) {
                return -smallestSacrifice * BOX_VALUE;
            }
            return 0;
        }

        private List<Integer> orderedMoves(boolean shuffle) {
            List<Integer> moves = board.legalMoves();
            if (shuffle) {
                Collections.shuffle(moves, random);
            }
            Collections.sort(moves, new Comparator<Integer>() {
                @Override
                public int compare(Integer first, Integer second) {
                    return Integer.compare(movePriority(second), movePriority(first));
                }
            });
            return moves;
        }

        private int movePriority(int moveIndex) {
            int captured = board.captureCount(moveIndex);
            int risk = board.riskCount(moveIndex);
            int safeBonus = captured == 0 && risk == 0 ? 10_000 : 0;
            return captured * 100_000 + safeBonus - risk * 500 + board.flexibility(moveIndex);
        }

        private List<Integer> limitedMoves(List<Integer> moves) {
            int limit = moves.size();
            if (board.remainingMoves > 64) {
                limit = Math.min(limit, 14);
            } else if (board.remainingMoves > 36) {
                limit = Math.min(limit, 20);
            }
            return limit == moves.size() ? moves : moves.subList(0, limit);
        }

        private void checkBudget() {
            nodes++;
            if (Thread.currentThread().isInterrupted() || nodes >= HARD_NODE_LIMIT
                    || (nodes & 127L) == 0L && System.nanoTime() >= deadline) {
                throw SearchTimeout.INSTANCE;
            }
        }
    }

    private enum Bound {
        EXACT,
        LOWER,
        UPPER
    }

    private static final class Transposition {
        private final int depth;
        private final int value;
        private final Bound bound;

        private Transposition(int depth, int value, Bound bound) {
            this.depth = depth;
            this.value = value;
            this.bound = bound;
        }
    }

    private static final class SearchTimeout extends RuntimeException {
        private static final SearchTimeout INSTANCE = new SearchTimeout();

        private SearchTimeout() {
            super(null, null, false, false);
        }
    }
}
