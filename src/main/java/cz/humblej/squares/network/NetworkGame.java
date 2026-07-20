package cz.humblej.squares.network;

import cz.humblej.squares.app.AppWindowSupport;
import cz.humblej.squares.codec.GameResultCodec;
import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.persistence.GameResultRecorder;
import cz.humblej.squares.persistence.StatisticsStore;
import cz.humblej.squares.ui.ChatPanel;
import cz.humblej.squares.ui.Messages;
import cz.humblej.squares.ui.SquaresPanel;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public final class NetworkGame {
    private NetworkGame() {
    }

    public static HostController host(JFrame frame, SquaresPanel panel, String hostAddress, int port,
                               PlayerProfile localProfile, GameResultRecorder recorder) {
        HostController controller = new HostController(frame, panel, hostAddress, port, localProfile, recorder);
        panel.setLocalPlayer(SquaresPanel.RED_PLAYER);
        panel.setClockEnabled(false);
        controller.refreshNetworkInfo();
        panel.setMoveHandler((horizontal, rowOrLine, columnOrLine) -> {
        });
        panel.setRestartHandler(() ->
                JOptionPane.showMessageDialog(panel, Messages.RESTART_WAITING_FOR_CLIENT,
                        Messages.RESTART_TITLE, JOptionPane.INFORMATION_MESSAGE));

        controller.startListening();
        return controller;
    }

    public static void join(JFrame frame, String host, int port,
                     PlayerProfile localProfile, GameResultRecorder recorder,
                     StatisticsStore statisticsStore) {
        Thread clientThread = new Thread(
                () -> NetworkClientSession.run(frame, host, port, localProfile, recorder, statisticsStore),
                "squares-client");
        clientThread.setDaemon(true);
        clientThread.start();
    }

    private static void runServer(JFrame frame, SquaresPanel panel, HostController controller) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(controller.port());
            controller.setServerSocket(serverSocket);

            try (Socket socket = serverSocket.accept();
                 BufferedReader input = NetworkProtocol.reader(socket);
                 PrintWriter output = NetworkProtocol.writer(socket)) {
                boolean[] hostReadyForNewGame = {false};
                boolean[] clientReadyForNewGame = {false};
                controller.setOutput(output);

                if (!verifyClientBuild(panel, input, output)) {
                    return;
                }

                output.println(NetworkProtocol.encodeProfile(controller.localProfile()));
                String clientProfileLine = input.readLine();
                PlayerProfile clientProfile = NetworkProtocol.decodeProfile(clientProfileLine);
                controller.setClientProfile(clientProfile);
                NetworkSwing.run(() -> {
                    panel.setPlayerProfiles(controller.localProfile(), clientProfile);
                    panel.resetGame();
                    panel.setClockEnabled(true);
                });

                output.println("SIZE " + panel.boardRows() + " " + panel.boardColumns());
                ChatPanel chat = createChatPanel(frame, panel, Messages.CHAT_HOST_TITLE, output);
                String clientAddress = socket.getInetAddress().getHostAddress();
                controller.setClientAddress(clientAddress);
                panel.setGameOverHandler(result ->
                        askHostForNewGame(panel, output, hostReadyForNewGame, clientReadyForNewGame,
                                controller.recorder(), result));
                panel.setRestartHandler(() -> askHostForRestart(panel, output));
                panel.setClockTickHandler(() -> sendState(panel, output));
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
                        askHostForClientRestart(panel, output, controller);
                    } else if ("RESTART_ACCEPT".equals(line)) {
                        restartNetworkGame(panel, output);
                    } else if ("RESTART_DECLINE".equals(line)) {
                        NetworkSwing.showInfo(panel, Messages.RESTART_DECLINED_BY_CLIENT, Messages.RESTART_TITLE);
                    } else if (line.startsWith("CHAT ")) {
                        NetworkSwing.receiveChat(chat, Messages.CHAT_CLIENT, line.substring("CHAT ".length()));
                    }
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            if (!controller.consumeExpectedServerClose()) {
                NetworkSwing.showError(Messages.NETWORK_HOST_ENDED, exception);
            }
        } finally {
            controller.setOutput(null);
            controller.clearServerSocket(serverSocket);

            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException exception) {
                    // Server is already ending; there is nothing useful to report here.
                }
            }
        }
    }

    private static boolean verifyClientBuild(SquaresPanel panel, BufferedReader input, PrintWriter output)
            throws IOException {
        String hostBuild = NetworkProtocol.buildId();
        output.println("BUILD " + NetworkProtocol.encodeValue(hostBuild));

        String line = input.readLine();

        if (line == null || !line.startsWith("BUILD ")) {
            output.println("BUILD_MISMATCH");
            NetworkSwing.showInfo(panel, Messages.NETWORK_INCOMPATIBLE_PROTOCOL, Messages.NETWORK_GAME_TITLE);
            return false;
        }

        String clientBuild = NetworkProtocol.decodeValue(line.substring("BUILD ".length()));

        if (!hostBuild.equals(clientBuild)) {
            output.println("BUILD_MISMATCH");
            NetworkSwing.showInfo(panel, Messages.buildMismatch(hostBuild, clientBuild), Messages.NETWORK_GAME_TITLE);
            return false;
        }

        output.println("BUILD_OK");
        return true;
    }

    private static ChatPanel createChatPanel(JFrame frame, SquaresPanel panel, String title, PrintWriter output) {
        ChatPanel[] chat = new ChatPanel[1];

        NetworkSwing.run(() -> {
            chat[0] = new ChatPanel(title,
                    ChatPanel.RED_MESSAGE_BACKGROUND,
                    ChatPanel.BLUE_MESSAGE_BACKGROUND,
                    message -> output.println("CHAT " + NetworkProtocol.encodeValue(message)));
            AppWindowSupport.showNetworkContent(frame, panel, chat[0]);
        });

        return chat[0];
    }

    private static void applyRemoteMove(SquaresPanel panel, PrintWriter output, String line) {
        String[] parts = line.split(" ");

        if (parts.length != 4) {
            return;
        }

        boolean horizontal = "H".equals(parts[1]);
        int rowOrLine = Integer.parseInt(parts[2]);
        int columnOrLine = Integer.parseInt(parts[3]);

        NetworkSwing.run(() -> {
            if (panel.getCurrentPlayer() == SquaresPanel.BLUE_PLAYER
                    && panel.applyMove(horizontal, rowOrLine, columnOrLine)) {
                sendState(panel, output);
            }
        });
    }

    private static void sendState(SquaresPanel panel, PrintWriter output) {
        NetworkSwing.run(() -> output.println("STATE " + panel.encodeState()));
    }

    private static void askHostForNewGame(SquaresPanel panel, PrintWriter output,
                                          boolean[] hostReadyForNewGame,
                                          boolean[] clientReadyForNewGame,
                                          GameResultRecorder recorder,
                                          GameResult result) {
        String message = Messages.gameOver(result);
        recorder.record(result);
        output.println("STATE " + panel.encodeState());
        try {
            output.println("GAME_RESULT " + GameResultCodec.encode(result));
        } catch (IOException exception) {
            NetworkSwing.showError(Messages.NETWORK_CONNECT_FAILED, exception);
            return;
        }

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

        NetworkSwing.run(() -> choice[0] = JOptionPane.showConfirmDialog(panel,
                Messages.RESTART_CONFIRM,
                Messages.RESTART_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE));

        if (choice[0] == JOptionPane.YES_OPTION) {
            output.println("RESTART_REQUEST_FROM_HOST");
        }
    }

    private static void askHostForClientRestart(SquaresPanel panel, PrintWriter output, HostController controller) {
        if (controller.isSettingsDialogOpen()) {
            output.println("RESTART_BUSY");
            return;
        }

        int[] choice = new int[1];

        NetworkSwing.run(() -> choice[0] = JOptionPane.showConfirmDialog(panel,
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

    private static void startNewGameIfBothConfirmed(SquaresPanel panel, PrintWriter output,
                                                    boolean[] hostReadyForNewGame,
                                                    boolean[] clientReadyForNewGame) {
        if (!hostReadyForNewGame[0] || !clientReadyForNewGame[0]) {
            return;
        }

        NetworkSwing.run(panel::resetGame);
        output.println("RESET");
        sendState(panel, output);
        hostReadyForNewGame[0] = false;
        clientReadyForNewGame[0] = false;
    }

    private static void restartNetworkGame(SquaresPanel panel, PrintWriter output) {
        NetworkSwing.run(panel::resetGame);
        output.println("RESET");
        sendState(panel, output);
    }

    public static final class HostController {
        private final JFrame frame;
        private final SquaresPanel panel;
        private final PlayerProfile localProfile;
        private final GameResultRecorder recorder;
        private String hostAddress;
        private int port;
        private volatile ServerSocket serverSocket;
        private volatile PrintWriter output;
        private volatile String clientAddress;
        private volatile PlayerProfile clientProfile;
        private volatile boolean settingsDialogOpen;
        private volatile boolean expectedServerClose;

        private HostController(JFrame frame, SquaresPanel panel, String hostAddress, int port,
                               PlayerProfile localProfile, GameResultRecorder recorder) {
            this.frame = frame;
            this.panel = panel;
            this.hostAddress = hostAddress;
            this.port = port;
            this.localProfile = localProfile;
            this.recorder = recorder;
        }

        private PlayerProfile localProfile() {
            return localProfile;
        }

        private GameResultRecorder recorder() {
            return recorder;
        }

        void startListening() {
            Thread serverThread = new Thread(() -> runServer(frame, panel, this), "squares-server");
            serverThread.setDaemon(true);
            serverThread.start();
        }

        public boolean changeNetworkEndpoint(String hostAddress, int port) {
            if (!canChangeNetworkEndpoint()) {
                return false;
            }

            boolean portChanged = this.port != port;
            this.hostAddress = hostAddress;
            this.port = port;
            refreshNetworkInfo();

            if (portChanged) {
                closeListeningSocket();
                startListening();
            }

            return true;
        }

        public boolean canChangeNetworkEndpoint() {
            return output == null && clientAddress == null;
        }

        public void changeSettings(int rows, int columns, int thinkingTimeLimitSeconds, boolean randomInitialEdgesEnabled) {
            NetworkSwing.run(() -> {
                panel.setThinkingTimeLimitSeconds(thinkingTimeLimitSeconds);
                panel.setRandomInitialEdgesEnabled(randomInitialEdgesEnabled);
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
                info += "\n" + Messages.waitingForClient();
            } else {
                info += "\n" + Messages.networkPlayersStatus(
                        localProfile.displayName(), clientProfile.displayName());
            }

            panel.setNetworkInfo(info);
        }

        public int boardSize() {
            return panel.boardRows();
        }

        public int thinkingTimeLimitSeconds() {
            return panel.thinkingTimeLimitSeconds();
        }

        public boolean randomInitialEdgesEnabled() {
            return panel.randomInitialEdgesEnabled();
        }

        public String hostAddress() {
            return hostAddress;
        }

        public int port() {
            return port;
        }

        public void setSettingsDialogOpen(boolean settingsDialogOpen) {
            this.settingsDialogOpen = settingsDialogOpen;
        }

        boolean isSettingsDialogOpen() {
            return settingsDialogOpen;
        }

        private void setOutput(PrintWriter output) {
            this.output = output;
        }

        private void setServerSocket(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        private void clearServerSocket(ServerSocket serverSocket) {
            if (this.serverSocket == serverSocket) {
                this.serverSocket = null;
            }
        }

        private boolean consumeExpectedServerClose() {
            if (!expectedServerClose) {
                return false;
            }

            expectedServerClose = false;
            return true;
        }

        private void closeListeningSocket() {
            ServerSocket socket = serverSocket;
            expectedServerClose = true;

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException exception) {
                    expectedServerClose = false;
                }
            }
        }

        private void setClientAddress(String clientAddress) {
            this.clientAddress = clientAddress;
            SwingUtilities.invokeLater(this::refreshNetworkInfo);
        }

        private void setClientProfile(PlayerProfile clientProfile) {
            this.clientProfile = clientProfile;
        }
    }
}
