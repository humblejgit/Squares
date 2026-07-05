package cz.codex.squares;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class NetworkGame {
    private NetworkGame() {
    }

    static HostController host(SquaresPanel panel, String hostAddress, int port) {
        HostController controller = new HostController(panel, hostAddress, port);
        panel.setLocalPlayer(SquaresPanel.RED_PLAYER);
        panel.setClockEnabled(false);
        controller.refreshNetworkInfo();
        panel.setMoveHandler((horizontal, rowOrLine, columnOrLine) -> {
        });
        panel.setRestartHandler(() ->
                JOptionPane.showMessageDialog(panel, Messages.RESTART_WAITING_FOR_CLIENT,
                        Messages.RESTART_TITLE, JOptionPane.INFORMATION_MESSAGE));

        Thread serverThread = new Thread(() -> runServer(panel, port, controller), "squares-server");
        serverThread.setDaemon(true);
        serverThread.start();
        return controller;
    }

    static void join(JFrame frame, String host, int port) {
        Thread clientThread = new Thread(() -> runClient(frame, host, port), "squares-client");
        clientThread.setDaemon(true);
        clientThread.start();
    }

    private static void runServer(SquaresPanel panel, int port, HostController controller) {
        try (ServerSocket serverSocket = new ServerSocket(port);
             Socket socket = serverSocket.accept();
             BufferedReader input = reader(socket);
             PrintWriter output = writer(socket)) {
            boolean[] hostReadyForNewGame = {false};
            boolean[] clientReadyForNewGame = {false};
            controller.setOutput(output);

            output.println("SIZE " + panel.boardRows() + " " + panel.boardColumns());
            String clientAddress = socket.getInetAddress().getHostAddress();
            controller.setClientAddress(clientAddress);
            panel.setClockEnabled(true);
            panel.setGameOverHandler(message ->
                    askHostForNewGame(panel, output, hostReadyForNewGame, clientReadyForNewGame, message));
            panel.setRestartHandler(() -> askHostForRestart(panel, output));
            panel.setMoveHandler((horizontal, rowOrLine, columnOrLine) -> {
                if (panel.applyMove(horizontal, rowOrLine, columnOrLine)) {
                    sendState(panel, output);
                }
            });
            sendState(panel, output);

            String line;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("MOVE ")) {
                    applyRemoteMove(panel, output, line);
                } else if ("GAME_OVER_ACK".equals(line)) {
                    clientReadyForNewGame[0] = true;
                    startNewGameIfBothConfirmed(panel, output, hostReadyForNewGame, clientReadyForNewGame);
                } else if ("RESTART_REQUEST".equals(line)) {
                    askHostForClientRestart(panel, output);
                } else if ("RESTART_ACCEPT".equals(line)) {
                    restartNetworkGame(panel, output);
                } else if ("RESTART_DECLINE".equals(line)) {
                    showMessage(panel, Messages.RESTART_DECLINED_BY_CLIENT, Messages.RESTART_TITLE);
                }
            }
        } catch (IOException exception) {
            showNetworkError(Messages.NETWORK_HOST_ENDED, exception);
        } finally {
            controller.setOutput(null);
        }
    }

    private static void runClient(JFrame frame, String host, int port) {
        try (Socket socket = new Socket(host, port);
             BufferedReader input = reader(socket);
             PrintWriter output = writer(socket)) {
            SquaresPanel[] panel = new SquaresPanel[1];

            String line;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("SIZE ")) {
                    panel[0] = createClientPanel(frame, host, port, output, line);
                } else if (line.startsWith("STATE ") && panel[0] != null) {
                    String state = line.substring("STATE ".length());
                    SwingUtilities.invokeLater(() -> panel[0].applyEncodedState(state));
                } else if (line.startsWith("GAME_OVER ") && panel[0] != null) {
                    String message = line.substring("GAME_OVER ".length());
                    showClientGameOver(panel[0], message);
                    output.println("GAME_OVER_ACK");
                } else if ("RESET".equals(line) && panel[0] != null) {
                    SwingUtilities.invokeLater(panel[0]::resetGame);
                } else if ("RESTART_REQUEST_FROM_HOST".equals(line) && panel[0] != null) {
                    askClientForHostRestart(panel[0], output);
                } else if ("RESTART_DECLINE".equals(line) && panel[0] != null) {
                    showMessage(panel[0], Messages.RESTART_DECLINED_BY_HOST, Messages.RESTART_TITLE);
                }
            }
        } catch (IOException exception) {
            showNetworkError(Messages.NETWORK_CONNECT_FAILED, exception);
        }
    }

    private static SquaresPanel createClientPanel(JFrame frame, String host, int port, PrintWriter output, String line) {
        String[] parts = line.split(" ");

        if (parts.length != 3) {
            throw new IllegalArgumentException(Messages.INVALID_SIZE_MESSAGE + line);
        }

        int rows = Integer.parseInt(parts[1]);
        int columns = Integer.parseInt(parts[2]);
        SquaresPanel[] createdPanel = new SquaresPanel[1];

        runOnEventThread(() -> {
            SquaresPanel panel = new SquaresPanel(rows, columns);
            panel.setLocalPlayer(SquaresPanel.BLUE_PLAYER);
            panel.setNetworkInfo(Messages.clientInfo(host, port, rows, columns) + "  |  " + Messages.connected());
            panel.setMoveHandler((horizontal, rowOrLine, columnOrLine) ->
                    output.println("MOVE " + edgeType(horizontal) + " " + rowOrLine + " " + columnOrLine));
            panel.setRestartHandler(() -> {
                output.println("RESTART_REQUEST");
                JOptionPane.showMessageDialog(panel,
                        Messages.RESTART_REQUEST_SENT,
                        Messages.RESTART_TITLE,
                        JOptionPane.INFORMATION_MESSAGE);
            });

            frame.setContentPane(panel);
            SquaresApp.fitWindowToContent(frame);
            createdPanel[0] = panel;
        });

        return createdPanel[0];
    }

    private static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private static PrintWriter writer(Socket socket) throws IOException {
        return new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private static void applyRemoteMove(SquaresPanel panel, PrintWriter output, String line) {
        String[] parts = line.split(" ");

        if (parts.length != 4) {
            return;
        }

        boolean horizontal = "H".equals(parts[1]);
        int rowOrLine = Integer.parseInt(parts[2]);
        int columnOrLine = Integer.parseInt(parts[3]);

        runOnEventThread(() -> {
            if (panel.getCurrentPlayer() == SquaresPanel.BLUE_PLAYER
                    && panel.applyMove(horizontal, rowOrLine, columnOrLine)) {
                sendState(panel, output);
            }
        });
    }

    private static void sendState(SquaresPanel panel, PrintWriter output) {
        runOnEventThread(() -> output.println("STATE " + panel.encodeState()));
    }

    private static void askHostForNewGame(SquaresPanel panel, PrintWriter output,
                                          boolean[] hostReadyForNewGame,
                                          boolean[] clientReadyForNewGame,
                                          String message) {
        output.println("STATE " + panel.encodeState());
        output.println("GAME_OVER " + message);

        int choice = JOptionPane.showConfirmDialog(panel,
                message + "\n\n" + Messages.NEW_GAME_PROMPT,
                Messages.GAME_OVER_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            hostReadyForNewGame[0] = true;
            startNewGameIfBothConfirmed(panel, output, hostReadyForNewGame, clientReadyForNewGame);
        }
    }

    private static void askHostForRestart(SquaresPanel panel, PrintWriter output) {
        int[] choice = new int[1];

        runOnEventThread(() -> choice[0] = JOptionPane.showConfirmDialog(panel,
                Messages.RESTART_CONFIRM,
                Messages.RESTART_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE));

        if (choice[0] == JOptionPane.YES_OPTION) {
            output.println("RESTART_REQUEST_FROM_HOST");
        }
    }

    private static void askHostForClientRestart(SquaresPanel panel, PrintWriter output) {
        int[] choice = new int[1];

        runOnEventThread(() -> choice[0] = JOptionPane.showConfirmDialog(panel,
                Messages.RESTART_REQUEST_FROM_CLIENT,
                Messages.RESTART_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE));

        if (choice[0] == JOptionPane.YES_OPTION) {
            restartNetworkGame(panel, output);
        } else {
            output.println("RESTART_DECLINE");
        }
    }

    private static void askClientForHostRestart(SquaresPanel panel, PrintWriter output) {
        int[] choice = new int[1];

        runOnEventThread(() -> choice[0] = JOptionPane.showConfirmDialog(panel,
                Messages.RESTART_REQUEST_FROM_HOST,
                Messages.RESTART_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE));

        output.println(choice[0] == JOptionPane.YES_OPTION ? "RESTART_ACCEPT" : "RESTART_DECLINE");
    }

    private static void showClientGameOver(SquaresPanel panel, String message) {
        runOnEventThread(() ->
                JOptionPane.showMessageDialog(panel, message, Messages.GAME_OVER_TITLE, JOptionPane.INFORMATION_MESSAGE));
    }

    private static void showMessage(SquaresPanel panel, String message, String title) {
        runOnEventThread(() -> JOptionPane.showMessageDialog(panel, message, title, JOptionPane.INFORMATION_MESSAGE));
    }

    private static void startNewGameIfBothConfirmed(SquaresPanel panel, PrintWriter output,
                                                    boolean[] hostReadyForNewGame,
                                                    boolean[] clientReadyForNewGame) {
        if (!hostReadyForNewGame[0] || !clientReadyForNewGame[0]) {
            return;
        }

        runOnEventThread(panel::resetGame);
        output.println("RESET");
        sendState(panel, output);
        hostReadyForNewGame[0] = false;
        clientReadyForNewGame[0] = false;
    }

    private static void restartNetworkGame(SquaresPanel panel, PrintWriter output) {
        runOnEventThread(panel::resetGame);
        output.println("RESET");
        sendState(panel, output);
    }

    private static void runOnEventThread(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(exception.getCause());
        }
    }

    private static String edgeType(boolean horizontal) {
        return horizontal ? "H" : "V";
    }

    private static void showNetworkError(String message, Exception exception) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, message + "\n\n" + exception.getMessage(),
                        Messages.NETWORK_GAME_TITLE, JOptionPane.WARNING_MESSAGE));
    }

    static final class HostController {
        private final SquaresPanel panel;
        private final String hostAddress;
        private final int port;
        private volatile PrintWriter output;
        private volatile String clientAddress;

        private HostController(SquaresPanel panel, String hostAddress, int port) {
            this.panel = panel;
            this.hostAddress = hostAddress;
            this.port = port;
        }

        void changeBoardSize(int rows, int columns) {
            runOnEventThread(() -> {
                panel.resizeBoard(rows, columns);
                refreshNetworkInfo();
            });

            PrintWriter writer = output;
            if (writer != null) {
                writer.println("SIZE " + rows + " " + columns);
                sendState(panel, writer);
            }
        }

        void refreshNetworkInfo() {
            String info = Messages.hostInfo(hostAddress, port, panel.boardRows(), panel.boardColumns());

            if (clientAddress == null) {
                info += "  |  " + Messages.waitingForClient();
            } else {
                info += "  |  " + Messages.clientConnected(clientAddress);
            }

            panel.setNetworkInfo(info);
        }

        private void setOutput(PrintWriter output) {
            this.output = output;
        }

        private void setClientAddress(String clientAddress) {
            this.clientAddress = clientAddress;
            SwingUtilities.invokeLater(this::refreshNetworkInfo);
        }
    }
}
