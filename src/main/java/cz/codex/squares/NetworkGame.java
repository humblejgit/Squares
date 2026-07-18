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
import java.util.Base64;

final class NetworkGame {
    private static final String PROTOCOL_ID = "profiles-and-results-1";

    private NetworkGame() {
    }

    static HostController host(JFrame frame, SquaresPanel panel, String hostAddress, int port,
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

    static void join(JFrame frame, String host, int port,
                     PlayerProfile localProfile, GameResultRecorder recorder,
                     StatisticsStore statisticsStore) {
        Thread clientThread = new Thread(
                () -> runClient(frame, host, port, localProfile, recorder, statisticsStore),
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
                 BufferedReader input = reader(socket);
                 PrintWriter output = writer(socket)) {
                boolean[] hostReadyForNewGame = {false};
                boolean[] clientReadyForNewGame = {false};
                controller.setOutput(output);

                if (!verifyClientBuild(panel, input, output)) {
                    return;
                }

                output.println(encodeProfile(controller.localProfile()));
                String clientProfileLine = input.readLine();
                PlayerProfile clientProfile = decodeProfile(clientProfileLine);
                controller.setClientProfile(clientProfile);
                runOnEventThread(() -> {
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
                        showMessage(panel, Messages.RESTART_DECLINED_BY_CLIENT, Messages.RESTART_TITLE);
                    } else if (line.startsWith("CHAT ")) {
                        receiveChatMessage(chat, Messages.CHAT_CLIENT, line);
                    }
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            if (!controller.consumeExpectedServerClose()) {
                showNetworkError(Messages.NETWORK_HOST_ENDED, exception);
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

    private static void runClient(JFrame frame, String host, int port,
                                  PlayerProfile localProfile, GameResultRecorder recorder,
                                  StatisticsStore statisticsStore) {
        try (Socket socket = new Socket(host, port);
             BufferedReader input = reader(socket);
             PrintWriter output = writer(socket)) {
            SquaresPanel[] panel = new SquaresPanel[1];
            ChatPanel[] chat = new ChatPanel[1];
            PlayerProfile[] hostProfile = new PlayerProfile[1];
            boolean buildVerified = false;

            String line;
            while ((line = input.readLine()) != null) {
                if (!buildVerified) {
                    if (!line.startsWith("BUILD ")) {
                        showNetworkMessage(frame, Messages.NETWORK_INCOMPATIBLE_PROTOCOL);
                        closeFrame(frame);
                        return;
                    }

                    String hostBuild = decodeNetworkValue(line.substring("BUILD ".length()));
                    output.println("BUILD " + encodeNetworkValue(networkBuildId()));
                    String buildResult = input.readLine();

                    if (!"BUILD_OK".equals(buildResult)) {
                        showNetworkMessage(frame, Messages.buildMismatch(hostBuild, networkBuildId()));
                        closeFrame(frame);
                        return;
                    }

                    hostProfile[0] = decodeProfile(input.readLine());
                    output.println(encodeProfile(localProfile));
                    buildVerified = true;
                } else if (line.startsWith("SIZE ")) {
                    panel[0] = createClientPanel(frame, host, port, output, line, chat,
                            hostProfile[0], localProfile, statisticsStore);
                } else if (line.startsWith("STATE ") && panel[0] != null) {
                    String state = line.substring("STATE ".length());
                    SwingUtilities.invokeLater(() -> panel[0].applyEncodedState(state));
                } else if (line.startsWith("GAME_RESULT ") && panel[0] != null) {
                    GameResult result = GameResultCodec.decode(line.substring("GAME_RESULT ".length()));
                    recorder.record(result);
                    showClientGameOver(panel[0], Messages.gameOver(result));
                    output.println("GAME_OVER_ACK");
                } else if ("RESET".equals(line) && panel[0] != null) {
                    SwingUtilities.invokeLater(panel[0]::resetGame);
                } else if ("RESTART_REQUEST_FROM_HOST".equals(line) && panel[0] != null) {
                    askClientForHostRestart(panel[0], output);
                } else if ("RESTART_DECLINE".equals(line) && panel[0] != null) {
                    showMessage(panel[0], Messages.RESTART_DECLINED_BY_HOST, Messages.RESTART_TITLE);
                } else if ("RESTART_BUSY".equals(line) && panel[0] != null) {
                    showMessage(panel[0], Messages.RESTART_HOST_BUSY, Messages.RESTART_TITLE);
                } else if (line.startsWith("CHAT ")) {
                    receiveChatMessage(chat[0], Messages.CHAT_HOST, line);
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            showNetworkError(Messages.NETWORK_CONNECT_FAILED, exception);
        }
    }

    private static SquaresPanel createClientPanel(JFrame frame, String host, int port, PrintWriter output, String line,
                                                   ChatPanel[] chat, PlayerProfile hostProfile,
                                                   PlayerProfile localProfile,
                                                   StatisticsStore statisticsStore) {
        String[] parts = line.split(" ");

        if (parts.length != 3) {
            throw new IllegalArgumentException(Messages.INVALID_SIZE_MESSAGE + line);
        }

        int rows = Integer.parseInt(parts[1]);
        int columns = Integer.parseInt(parts[2]);
        SquaresPanel[] createdPanel = new SquaresPanel[1];

        runOnEventThread(() -> {
            SquaresPanel panel = new SquaresPanel(rows, columns);
            panel.setPlayerProfiles(hostProfile, localProfile);
            panel.setLocalPlayer(SquaresPanel.BLUE_PLAYER);
            panel.setClockEnabled(false);
            panel.setNetworkInfo(Messages.clientInfo(host, port, rows, columns) + "\n"
                    + Messages.networkPlayersStatus(hostProfile.displayName(), localProfile.displayName()));
            panel.setMoveHandler((horizontal, rowOrLine, columnOrLine) ->
                    output.println("MOVE " + edgeType(horizontal) + " " + rowOrLine + " " + columnOrLine));
            panel.setRestartHandler(() -> {
                output.println("RESTART_REQUEST");
                JOptionPane.showMessageDialog(panel,
                        Messages.RESTART_REQUEST_SENT,
                        Messages.RESTART_TITLE,
                        JOptionPane.INFORMATION_MESSAGE);
            });

            if (chat[0] == null) {
                chat[0] = new ChatPanel(Messages.CHAT_CLIENT_TITLE,
                        ChatPanel.BLUE_MESSAGE_BACKGROUND,
                        ChatPanel.RED_MESSAGE_BACKGROUND,
                        message -> output.println("CHAT " + encodeChatMessage(message)));
            }

            SquaresApp.showNetworkContent(frame, panel, chat[0]);
            SquaresApp.installNetworkClientGameMenu(frame, panel, statisticsStore, localProfile);
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

    private static boolean verifyClientBuild(SquaresPanel panel, BufferedReader input, PrintWriter output)
            throws IOException {
        String hostBuild = networkBuildId();
        output.println("BUILD " + encodeNetworkValue(hostBuild));

        String line = input.readLine();

        if (line == null || !line.startsWith("BUILD ")) {
            output.println("BUILD_MISMATCH");
            showMessage(panel, Messages.NETWORK_INCOMPATIBLE_PROTOCOL, Messages.NETWORK_GAME_TITLE);
            return false;
        }

        String clientBuild = decodeNetworkValue(line.substring("BUILD ".length()));

        if (!hostBuild.equals(clientBuild)) {
            output.println("BUILD_MISMATCH");
            showMessage(panel, Messages.buildMismatch(hostBuild, clientBuild), Messages.NETWORK_GAME_TITLE);
            return false;
        }

        output.println("BUILD_OK");
        return true;
    }

    private static String networkBuildId() {
        return BuildInfo.buildId() + "/" + PROTOCOL_ID;
    }

    private static ChatPanel createChatPanel(JFrame frame, SquaresPanel panel, String title, PrintWriter output) {
        ChatPanel[] chat = new ChatPanel[1];

        runOnEventThread(() -> {
            chat[0] = new ChatPanel(title,
                    ChatPanel.RED_MESSAGE_BACKGROUND,
                    ChatPanel.BLUE_MESSAGE_BACKGROUND,
                    message -> output.println("CHAT " + encodeChatMessage(message)));
            SquaresApp.showNetworkContent(frame, panel, chat[0]);
        });

        return chat[0];
    }

    private static void receiveChatMessage(ChatPanel chat, String sender, String line) {
        String message = decodeChatMessage(line.substring("CHAT ".length()));
        runOnEventThread(() -> {
            if (chat != null) {
                chat.receive(sender, message);
            }
        });
    }

    private static String encodeChatMessage(String message) {
        return encodeNetworkValue(message);
    }

    private static String decodeChatMessage(String encoded) {
        return decodeNetworkValue(encoded);
    }

    private static String encodeNetworkValue(String message) {
        return Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeNetworkValue(String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private static String encodeProfile(PlayerProfile profile) {
        return "PROFILE " + profile.id() + " " + profile.createdAt().toEpochMilli() + " "
                + encodeNetworkValue(profile.displayName());
    }

    private static PlayerProfile decodeProfile(String line) {
        if (line == null || !line.startsWith("PROFILE ")) {
            throw new IllegalArgumentException(Messages.NETWORK_INCOMPATIBLE_PROTOCOL);
        }

        String[] parts = line.split(" ", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException(Messages.NETWORK_INCOMPATIBLE_PROTOCOL);
        }

        return new PlayerProfile(java.util.UUID.fromString(parts[1]),
                decodeNetworkValue(parts[3]),
                java.time.Instant.ofEpochMilli(Long.parseLong(parts[2])),
                false);
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
                                          GameResultRecorder recorder,
                                          GameResult result) {
        String message = Messages.gameOver(result);
        recorder.record(result);
        output.println("STATE " + panel.encodeState());
        try {
            output.println("GAME_RESULT " + GameResultCodec.encode(result));
        } catch (IOException exception) {
            showNetworkError(Messages.NETWORK_CONNECT_FAILED, exception);
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

        runOnEventThread(() -> choice[0] = JOptionPane.showConfirmDialog(panel,
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

    private static void showNetworkMessage(JFrame frame, String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(frame, message, Messages.NETWORK_GAME_TITLE, JOptionPane.WARNING_MESSAGE));
    }

    private static void closeFrame(JFrame frame) {
        SwingUtilities.invokeLater(frame::dispose);
    }

    static final class HostController {
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

        boolean changeNetworkEndpoint(String hostAddress, int port) {
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

        boolean canChangeNetworkEndpoint() {
            return output == null && clientAddress == null;
        }

        void changeSettings(int rows, int columns, int thinkingTimeLimitSeconds, boolean randomInitialEdgesEnabled) {
            runOnEventThread(() -> {
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

        int boardSize() {
            return panel.boardRows();
        }

        int thinkingTimeLimitSeconds() {
            return panel.thinkingTimeLimitSeconds();
        }

        boolean randomInitialEdgesEnabled() {
            return panel.randomInitialEdgesEnabled();
        }

        String hostAddress() {
            return hostAddress;
        }

        int port() {
            return port;
        }

        void setSettingsDialogOpen(boolean settingsDialogOpen) {
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
