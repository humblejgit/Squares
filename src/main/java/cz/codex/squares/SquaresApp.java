package cz.codex.squares;

import javax.swing.JFrame;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class SquaresApp {
    private SquaresApp() {
    }

    public static void main(String[] args) {
        localizeOptionPaneButtons();
        SwingUtilities.invokeLater(SquaresApp::showWindow);
    }

    private static void localizeOptionPaneButtons() {
        UIManager.put("OptionPane.yesButtonText", Messages.OPTION_YES);
        UIManager.put("OptionPane.noButtonText", Messages.OPTION_NO);
        UIManager.put("OptionPane.okButtonText", Messages.OPTION_OK);
        UIManager.put("OptionPane.cancelButtonText", Messages.OPTION_CANCEL);
    }

    private static void showWindow() {
        int gameMode = askGameMode();

        if (gameMode == 2) {
            showClientWindow();
            return;
        }

        int boardSize = askBoardSize(null);
        JFrame frame = new JFrame(Messages.APP_TITLE);
        SquaresPanel panel = new SquaresPanel(boardSize, boardSize);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        configureGameMode(frame, panel, boardSize, gameMode);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showClientWindow() {
        String host = JOptionPane.showInputDialog(null, Messages.HOST_ADDRESS_PROMPT, "127.0.0.1");

        if (isBlank(host)) {
            return;
        }

        int port = askPort(null);
        JFrame frame = new JFrame(Messages.WINDOW_CLIENT);
        JLabel connectingLabel = new JLabel(Messages.CONNECTING_TO_HOST, SwingConstants.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(connectingLabel);
        frame.setSize(360, 140);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        NetworkGame.join(frame, host.trim(), port);
    }

    private static int askGameMode() {
        String[] options = {Messages.GAME_MODE_LOCAL, Messages.GAME_MODE_HOST, Messages.GAME_MODE_JOIN};
        int choice = JOptionPane.showOptionDialog(null, Messages.GAME_MODE_PROMPT, Messages.APP_TITLE,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        return choice < 0 ? 0 : choice;
    }

    private static void configureGameMode(JFrame frame, SquaresPanel panel, int boardSize, int gameMode) {
        if (gameMode == 1) {
            NetworkAddress networkAddress = askNetworkAddress(frame);
            int port = askPort(frame);
            frame.setTitle(Messages.WINDOW_HOST);
            panel.setNetworkInfo(Messages.hostInfo(networkAddress.address(), port, boardSize, boardSize));
            NetworkGame.HostController hostController = NetworkGame.host(panel, networkAddress.address(), port);
            installGameMenu(frame, () -> askHostForBoardSizeChange(frame, hostController));
            JOptionPane.showMessageDialog(frame,
                    Messages.hostStarted(port, networkAddress.address(), boardSize, boardSize),
                    Messages.APP_TITLE, JOptionPane.INFORMATION_MESSAGE);
        } else {
            panel.setLocalPlayer(SquaresPanel.NO_PLAYER);
            panel.setNetworkInfo(Messages.localInfo(boardSize, boardSize));
            panel.setGameOverHandler(message -> askForNewLocalGame(frame, panel, message));
            panel.setRestartHandler(() -> askForLocalRestart(frame, panel));
            installGameMenu(frame, () -> askLocalForBoardSizeChange(frame, panel));
            frame.setTitle(Messages.WINDOW_LOCAL);
        }
    }

    private static void askForLocalRestart(JFrame frame, SquaresPanel panel) {
        int choice = JOptionPane.showConfirmDialog(frame,
                Messages.RESTART_CONFIRM,
                Messages.RESTART_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            panel.resetGame();
        }
    }

    private static int askBoardSize(JFrame frame) {
        String[] sizes = {"5x5", "6x6", "7x7", "8x8", "9x9", "10x10"};
        String selected = (String) JOptionPane.showInputDialog(frame,
                Messages.BOARD_SIZE_PROMPT,
                Messages.BOARD_SIZE_TITLE,
                JOptionPane.PLAIN_MESSAGE,
                null,
                sizes,
                sizes[0]);

        if (isBlank(selected)) {
            return 5;
        }

        return Integer.parseInt(selected.substring(0, selected.indexOf('x')));
    }

    private static void installGameMenu(JFrame frame, Runnable changeSizeAction) {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu(Messages.MENU_GAME);
        JMenuItem changeSizeItem = new JMenuItem(Messages.MENU_CHANGE_SIZE);
        JCheckBoxMenuItem soundsItem = new JCheckBoxMenuItem(Messages.MENU_SOUNDS, SoundPlayer.isEnabled());
        JMenuItem exitItem = new JMenuItem(Messages.MENU_EXIT);

        changeSizeItem.addActionListener(event -> changeSizeAction.run());
        soundsItem.addActionListener(event -> SoundPlayer.setEnabled(soundsItem.isSelected()));
        exitItem.addActionListener(event -> exitApplication(frame));
        gameMenu.add(changeSizeItem);
        gameMenu.add(soundsItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        menuBar.add(gameMenu);
        frame.setJMenuBar(menuBar);
        frame.revalidate();
        frame.repaint();
        frame.pack();
    }

    private static void askHostForBoardSizeChange(JFrame frame, NetworkGame.HostController hostController) {
        int boardSize = askBoardSize(frame);
        int choice = JOptionPane.showConfirmDialog(frame,
                Messages.changeSizeConfirm(boardSize, boardSize),
                Messages.CHANGE_SIZE_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            hostController.changeBoardSize(boardSize, boardSize);
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationRelativeTo(null);
        }
    }

    private static void askLocalForBoardSizeChange(JFrame frame, SquaresPanel panel) {
        int boardSize = askBoardSize(frame);
        int choice = JOptionPane.showConfirmDialog(frame,
                Messages.changeSizeConfirm(boardSize, boardSize),
                Messages.CHANGE_SIZE_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            panel.resizeBoard(boardSize, boardSize);
            panel.setNetworkInfo(Messages.localInfo(boardSize, boardSize));
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationRelativeTo(null);
        }
    }

    private static void exitApplication(JFrame frame) {
        frame.dispose();
        System.exit(0);
    }

    private static void askForNewLocalGame(JFrame frame, SquaresPanel panel, String message) {
        int choice = JOptionPane.showConfirmDialog(frame,
                message + "\n\n" + Messages.NEW_GAME_PROMPT,
                Messages.GAME_OVER_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            panel.resetGame();
        }
    }

    private static int askPort(JFrame frame) {
        String value = JOptionPane.showInputDialog(frame, Messages.PORT_PROMPT, "5000");

        if (isBlank(value)) {
            return 5000;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 5000;
        }
    }

    private static NetworkAddress askNetworkAddress(JFrame frame) {
        List<NetworkAddress> addresses = localIpAddresses();

        if (addresses.isEmpty()) {
            return new NetworkAddress("127.0.0.1", "localhost");
        }

        NetworkAddress selected = (NetworkAddress) JOptionPane.showInputDialog(frame,
                Messages.ADAPTER_PROMPT,
                Messages.ADAPTER_TITLE,
                JOptionPane.PLAIN_MESSAGE,
                null,
                addresses.toArray(),
                addresses.get(0));

        return selected == null ? addresses.get(0) : selected;
    }

    private static List<NetworkAddress> localIpAddresses() {
        List<NetworkAddress> addresses = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();

                while (interfaceAddresses.hasMoreElements()) {
                    InetAddress address = interfaceAddresses.nextElement();
                    String value = address.getHostAddress();

                    if (!address.isLoopbackAddress() && value.indexOf(':') < 0) {
                        String label = networkInterface.getDisplayName();

                        if (networkInterface.isVirtual()) {
                            label += Messages.VIRTUAL_ADAPTER_SUFFIX;
                        }

                        addresses.add(new NetworkAddress(value, label));
                    }
                }
            }
        } catch (SocketException exception) {
            addresses.add(new NetworkAddress("127.0.0.1", "localhost"));
        }

        return addresses;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class NetworkAddress {
        private final String address;
        private final String adapterName;

        private NetworkAddress(String address, String adapterName) {
            this.address = address;
            this.adapterName = adapterName;
        }

        private String address() {
            return address;
        }

        @Override
        public String toString() {
            return address + " - " + adapterName;
        }
    }
}
