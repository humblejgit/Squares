package cz.humblej.squares.network;

import cz.humblej.squares.app.AppWindowSupport;
import cz.humblej.squares.app.SquaresApp;
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
import java.net.Socket;

final class NetworkClientSession {
    private NetworkClientSession() {
    }

    static void run(JFrame frame, String host, int port, PlayerProfile localProfile,
                    GameResultRecorder recorder, StatisticsStore statisticsStore) {
        try (Socket socket = new Socket(host, port);
             BufferedReader input = NetworkProtocol.reader(socket);
             PrintWriter output = NetworkProtocol.writer(socket)) {
            SquaresPanel[] panel = new SquaresPanel[1];
            ChatPanel[] chat = new ChatPanel[1];
            PlayerProfile[] hostProfile = new PlayerProfile[1];
            boolean buildVerified = false;

            String line;
            while ((line = input.readLine()) != null) {
                if (!buildVerified) {
                    if (!verifyHostBuild(frame, input, output, line)) {
                        return;
                    }
                    hostProfile[0] = NetworkProtocol.decodeProfile(input.readLine());
                    output.println(NetworkProtocol.encodeProfile(localProfile));
                    buildVerified = true;
                } else if (line.startsWith("SIZE ")) {
                    panel[0] = createPanel(frame, host, port, output, line, chat,
                            hostProfile[0], localProfile, statisticsStore);
                } else if (line.startsWith("STATE ") && panel[0] != null) {
                    String state = line.substring("STATE ".length());
                    SwingUtilities.invokeLater(() -> panel[0].applyEncodedState(state));
                } else if (line.startsWith("GAME_RESULT ") && panel[0] != null) {
                    GameResult result = GameResultCodec.decode(line.substring("GAME_RESULT ".length()));
                    recorder.record(result);
                    NetworkSwing.showInfo(panel[0], Messages.gameOver(result), Messages.GAME_OVER_TITLE);
                    output.println("GAME_OVER_ACK");
                } else if ("RESET".equals(line) && panel[0] != null) {
                    SwingUtilities.invokeLater(panel[0]::resetGame);
                } else if ("RESTART_REQUEST_FROM_HOST".equals(line) && panel[0] != null) {
                    askForHostRestart(panel[0], output);
                } else if ("RESTART_DECLINE".equals(line) && panel[0] != null) {
                    NetworkSwing.showInfo(panel[0], Messages.RESTART_DECLINED_BY_HOST, Messages.RESTART_TITLE);
                } else if ("RESTART_BUSY".equals(line) && panel[0] != null) {
                    NetworkSwing.showInfo(panel[0], Messages.RESTART_HOST_BUSY, Messages.RESTART_TITLE);
                } else if (line.startsWith("CHAT ")) {
                    NetworkSwing.receiveChat(chat[0], Messages.CHAT_HOST, line.substring("CHAT ".length()));
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            NetworkSwing.showError(Messages.NETWORK_CONNECT_FAILED, exception);
        }
    }

    private static boolean verifyHostBuild(JFrame frame, BufferedReader input, PrintWriter output, String line)
            throws IOException {
        if (!line.startsWith("BUILD ")) {
            NetworkSwing.showWarning(frame, Messages.NETWORK_INCOMPATIBLE_PROTOCOL);
            NetworkSwing.close(frame);
            return false;
        }

        String hostBuild = NetworkProtocol.decodeValue(line.substring("BUILD ".length()));
        output.println("BUILD " + NetworkProtocol.encodeValue(NetworkProtocol.buildId()));
        String buildResult = input.readLine();
        if (!"BUILD_OK".equals(buildResult)) {
            NetworkSwing.showWarning(frame, Messages.buildMismatch(hostBuild, NetworkProtocol.buildId()));
            NetworkSwing.close(frame);
            return false;
        }
        return true;
    }

    private static SquaresPanel createPanel(JFrame frame, String host, int port, PrintWriter output, String line,
                                             ChatPanel[] chat, PlayerProfile hostProfile,
                                             PlayerProfile localProfile, StatisticsStore statisticsStore) {
        String[] parts = line.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException(Messages.INVALID_SIZE_MESSAGE + line);
        }

        int rows = Integer.parseInt(parts[1]);
        int columns = Integer.parseInt(parts[2]);
        SquaresPanel[] createdPanel = new SquaresPanel[1];
        NetworkSwing.run(() -> {
            SquaresPanel panel = new SquaresPanel(rows, columns);
            panel.setPlayerProfiles(hostProfile, localProfile);
            panel.setLocalPlayer(SquaresPanel.BLUE_PLAYER);
            panel.setClockEnabled(false);
            panel.setNetworkInfo(Messages.clientInfo(host, port, rows, columns) + "\n"
                    + Messages.networkPlayersStatus(hostProfile.displayName(), localProfile.displayName()));
            panel.setMoveHandler((horizontal, rowOrLine, columnOrLine) -> output.println(
                    "MOVE " + NetworkProtocol.edgeType(horizontal) + " " + rowOrLine + " " + columnOrLine));
            panel.setRestartHandler(() -> {
                output.println("RESTART_REQUEST");
                JOptionPane.showMessageDialog(panel, Messages.RESTART_REQUEST_SENT,
                        Messages.RESTART_TITLE, JOptionPane.INFORMATION_MESSAGE);
            });

            if (chat[0] == null) {
                chat[0] = new ChatPanel(Messages.CHAT_CLIENT_TITLE,
                        ChatPanel.BLUE_MESSAGE_BACKGROUND, ChatPanel.RED_MESSAGE_BACKGROUND,
                        message -> output.println("CHAT " + NetworkProtocol.encodeValue(message)));
            }

            AppWindowSupport.showNetworkContent(frame, panel, chat[0]);
            SquaresApp.installNetworkClientGameMenu(frame, panel, statisticsStore, localProfile);
            createdPanel[0] = panel;
        });
        return createdPanel[0];
    }

    private static void askForHostRestart(SquaresPanel panel, PrintWriter output) {
        int[] choice = new int[1];
        NetworkSwing.run(() -> choice[0] = JOptionPane.showConfirmDialog(panel,
                Messages.RESTART_REQUEST_FROM_HOST, Messages.RESTART_TITLE,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE));
        output.println(choice[0] == JOptionPane.YES_OPTION ? "RESTART_ACCEPT" : "RESTART_DECLINE");
    }
}
