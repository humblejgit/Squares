package cz.humblej.squares.ui;

import cz.humblej.squares.game.ComputerStrategy;

import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class ComputerMoveController {
    private Timer moveTimer;
    private SwingWorker<ComputerStrategy.Decision, Void> moveWorker;
    private long generation;

    void schedule(int rows, int columns, int[][] horizontalEdges, int[][] verticalEdges,
                  double skill, Random random, BooleanSupplier moveStillValid,
                  Consumer<ComputerStrategy.Move> moveConsumer) {
        stop();
        final long scheduledGeneration = generation;
        final long thinkingStarted = System.nanoTime();
        final int[][] horizontalSnapshot = copyEdges(horizontalEdges);
        final int[][] verticalSnapshot = copyEdges(verticalEdges);

        moveWorker = new SwingWorker<ComputerStrategy.Decision, Void>() {
            @Override
            protected ComputerStrategy.Decision doInBackground() {
                return ComputerStrategy.chooseDecision(rows, columns, horizontalSnapshot,
                        verticalSnapshot, skill, random);
            }

            @Override
            protected void done() {
                if (moveWorker == this) {
                    moveWorker = null;
                }
                if (isCancelled() || scheduledGeneration != generation || !moveStillValid.getAsBoolean()) {
                    return;
                }

                try {
                    ComputerStrategy.Decision decision = get();
                    if (decision.move == null) {
                        return;
                    }

                    long elapsedMillis = Math.max(0L, (System.nanoTime() - thinkingStarted) / 1_000_000L);
                    int remainingDelay = (int) Math.max(0L, decision.thinkingDelayMillis - elapsedMillis);
                    moveTimer = new Timer(remainingDelay, event -> {
                        moveTimer = null;
                        if (scheduledGeneration == generation && moveStillValid.getAsBoolean()) {
                            moveConsumer.accept(decision.move);
                        }
                    });
                    moveTimer.setRepeats(false);
                    moveTimer.start();
                } catch (CancellationException ignored) {
                    // A restart, settings change or finished game invalidated this calculation.
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    exception.getCause().printStackTrace();
                }
            }
        };
        moveWorker.execute();
    }

    void stop() {
        generation++;

        if (moveTimer != null) {
            moveTimer.stop();
            moveTimer = null;
        }
        if (moveWorker != null) {
            moveWorker.cancel(true);
            moveWorker = null;
        }
    }

    private static int[][] copyEdges(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }
}
