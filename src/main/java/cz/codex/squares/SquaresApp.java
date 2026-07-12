package cz.codex.squares;

import javax.swing.JFrame;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class SquaresApp {
    private static final String CLOCK_PAUSE_LISTENER_PROPERTY = "squares.clockPauseListener";
    private static final int DEFAULT_NETWORK_PORT = 1080;
    private static final int[] THINK_TIME_SECONDS = {0, 30, 60, 120, 300};

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

        GameOptions gameOptions = askGameOptions(null, 8, 0, false);

        if (gameOptions == null) {
            return;
        }

        int boardSize = gameOptions.boardSize();
        JFrame frame = new JFrame(Messages.APP_TITLE);
        SquaresPanel panel = new SquaresPanel(boardSize, boardSize);
        panel.setThinkingTimeLimitSeconds(gameOptions.thinkingTimeSeconds());
        panel.setRandomInitialEdgesEnabled(gameOptions.randomEdges());
        panel.resetGame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        if (!configureGameMode(frame, panel, boardSize, gameMode)) {
            frame.dispose();
            return;
        }
        fitWindowToContent(frame);
        frame.setVisible(true);
    }

    private static void showClientWindow() {
        String host = JOptionPane.showInputDialog(null, Messages.HOST_ADDRESS_PROMPT, "127.0.0.1");

        if (isBlank(host)) {
            return;
        }

        int port = DEFAULT_NETWORK_PORT;
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

    private static boolean configureGameMode(JFrame frame, SquaresPanel panel, int boardSize, int gameMode) {
        if (gameMode == 1) {
            panel.setClockEnabled(false);
            panel.resetClock();
            NetworkAddress networkAddress = askNetworkAddress(frame);
            if (networkAddress == null) {
                JOptionPane.showMessageDialog(frame, Messages.NO_NETWORK_ADAPTER,
                        Messages.ADAPTER_TITLE, JOptionPane.WARNING_MESSAGE);
                return false;
            }

            int port = DEFAULT_NETWORK_PORT;
            frame.setTitle(Messages.WINDOW_HOST);
            panel.setNetworkInfo(Messages.hostInfo(networkAddress.address(), port, boardSize, boardSize));
            NetworkGame.HostController hostController = NetworkGame.host(frame, panel, networkAddress.address(), port);
            installGameMenu(frame, panel, false, () -> askHostForSettingsChange(frame, hostController));
            return true;
        } else {
            panel.setLocalPlayer(SquaresPanel.NO_PLAYER);
            panel.setNetworkInfo(Messages.localInfo(boardSize, boardSize));
            panel.setGameOverHandler(message -> askForNewLocalGame(frame, panel, message));
            panel.setRestartHandler(() -> askForLocalRestart(frame, panel));
            installWindowPauseHandling(frame, panel);
            installGameMenu(frame, panel, true, () -> askLocalForSettingsChange(frame, panel));
            frame.setTitle(Messages.WINDOW_LOCAL);
            return true;
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

    private static GameOptions askGameOptions(JFrame frame, int selectedBoardSize, int selectedThinkingTimeSeconds,
                                              boolean selectedRandomEdges) {
        String[] sizes = {"5x5", "6x6", "7x7", "8x8", "9x9", "10x10"};
        String[] thinkTimes = {
                Messages.THINK_TIME_NONE,
                "30 s",
                "1 min",
                "2 min",
                "5 min"
        };
        JComboBox<String> sizeBox = new JComboBox<>(sizes);
        JComboBox<String> thinkTimeBox = new JComboBox<>(thinkTimes);
        JCheckBox randomEdgesBox = new JCheckBox(Messages.GAME_OPTIONS_RANDOM_EDGES);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        sizeBox.setSelectedItem(selectedBoardSize + "x" + selectedBoardSize);
        thinkTimeBox.setSelectedIndex(thinkTimeIndex(selectedThinkingTimeSeconds));
        randomEdgesBox.setSelected(selectedRandomEdges);

        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel(Messages.GAME_OPTIONS_BOARD_SIZE), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sizeBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(Messages.GAME_OPTIONS_THINK_TIME), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(thinkTimeBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(randomEdgesBox, constraints);

        int choice = JOptionPane.showConfirmDialog(frame,
                panel,
                Messages.GAME_OPTIONS_TITLE,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String selectedSize = (String) sizeBox.getSelectedItem();
        int boardSize = selectedSize == null ? 5 : Integer.parseInt(selectedSize.substring(0, selectedSize.indexOf('x')));
        return new GameOptions(boardSize, thinkingTimeSeconds(thinkTimeBox.getSelectedIndex()),
                randomEdgesBox.isSelected());
    }

    private static void installGameMenu(JFrame frame, SquaresPanel panel, boolean pauseClockForDialogs,
                                        Runnable changeSizeAction) {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu(Messages.MENU_GAME);
        JMenuItem settingsItem = new JMenuItem(Messages.MENU_SETTINGS);
        JCheckBoxMenuItem soundsItem = new JCheckBoxMenuItem(Messages.MENU_SOUNDS, SoundPlayer.isEnabled());
        JMenuItem aboutItem = new JMenuItem(Messages.MENU_ABOUT);
        JMenuItem exitItem = new JMenuItem(Messages.MENU_EXIT);

        settingsItem.addActionListener(event -> changeSizeAction.run());
        soundsItem.addActionListener(event -> SoundPlayer.setEnabled(soundsItem.isSelected()));
        aboutItem.addActionListener(event -> showAbout(frame, panel, pauseClockForDialogs));
        exitItem.addActionListener(event -> exitApplication(frame));
        gameMenu.add(settingsItem);
        gameMenu.add(soundsItem);
        gameMenu.addSeparator();
        gameMenu.add(aboutItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        menuBar.add(gameMenu);
        frame.setJMenuBar(menuBar);
        frame.revalidate();
        frame.repaint();
        fitWindowToContent(frame);
    }

    private static void askHostForSettingsChange(JFrame frame, NetworkGame.HostController hostController) {
        hostController.setSettingsDialogOpen(true);

        try {
            HostSettings hostSettings = askHostSettings(frame, hostController);

            if (hostSettings == null) {
                return;
            }

            int boardSize = hostSettings.gameOptions().boardSize();
            int choice = JOptionPane.showConfirmDialog(frame,
                    Messages.settingsRestartConfirm(),
                    Messages.CHANGE_SIZE_TITLE,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                if (hostSettings.networkChanged(hostController.hostAddress(), hostController.port())
                        && !hostController.changeNetworkEndpoint(hostSettings.hostAddress(), hostSettings.port())) {
                    JOptionPane.showMessageDialog(frame,
                            Messages.NETWORK_SETTINGS_ACTIVE_CLIENT,
                            Messages.NETWORK_GAME_TITLE,
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                hostController.changeSettings(boardSize, boardSize, hostSettings.gameOptions().thinkingTimeSeconds(),
                        hostSettings.gameOptions().randomEdges());
                fitWindowToContent(frame);
            }
        } finally {
            hostController.setSettingsDialogOpen(false);
        }
    }

    private static HostSettings askHostSettings(JFrame frame, NetworkGame.HostController hostController) {
        String[] sizes = {"5x5", "6x6", "7x7", "8x8", "9x9", "10x10"};
        String[] thinkTimes = {
                Messages.THINK_TIME_NONE,
                "30 s",
                "1 min",
                "2 min",
                "5 min"
        };
        JComboBox<String> sizeBox = new JComboBox<>(sizes);
        JComboBox<String> thinkTimeBox = new JComboBox<>(thinkTimes);
        JCheckBox randomEdgesBox = new JCheckBox(Messages.GAME_OPTIONS_RANDOM_EDGES);
        List<NetworkAddress> addresses = localIpAddresses(true);
        if (!hasAddress(addresses, hostController.hostAddress())) {
            addresses.add(0, new NetworkAddress(hostController.hostAddress(), Messages.CURRENT_NETWORK_ADDRESS));
        }
        JComboBox<NetworkAddress> adapterBox = new JComboBox<>(addresses.toArray(new NetworkAddress[0]));
        JTextField portField = new JTextField(String.valueOf(hostController.port()), 8);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        sizeBox.setSelectedItem(hostController.boardSize() + "x" + hostController.boardSize());
        thinkTimeBox.setSelectedIndex(thinkTimeIndex(hostController.thinkingTimeLimitSeconds()));
        randomEdgesBox.setSelected(hostController.randomInitialEdgesEnabled());
        selectAddress(adapterBox, hostController.hostAddress());
        adapterBox.setEnabled(hostController.canChangeNetworkEndpoint());
        portField.setEnabled(hostController.canChangeNetworkEndpoint());

        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel(Messages.GAME_OPTIONS_BOARD_SIZE), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sizeBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(Messages.GAME_OPTIONS_THINK_TIME), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(thinkTimeBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(randomEdgesBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        panel.add(new JLabel(Messages.NETWORK_SETTINGS_ADAPTER), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(adapterBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(Messages.NETWORK_SETTINGS_PORT), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(portField, constraints);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(frame,
                    panel,
                    Messages.GAME_OPTIONS_TITLE,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }

            Integer port = parsePort(portField.getText());
            if (port == null) {
                JOptionPane.showMessageDialog(frame,
                        Messages.INVALID_PORT,
                        Messages.NETWORK_GAME_TITLE,
                        JOptionPane.WARNING_MESSAGE);
                continue;
            }

            NetworkAddress selectedAddress = (NetworkAddress) adapterBox.getSelectedItem();
            if (selectedAddress == null) {
                JOptionPane.showMessageDialog(frame,
                        Messages.NO_NETWORK_ADAPTER,
                        Messages.ADAPTER_TITLE,
                        JOptionPane.WARNING_MESSAGE);
                continue;
            }

            String selectedSize = (String) sizeBox.getSelectedItem();
            int boardSize = selectedSize == null ? 5 : Integer.parseInt(selectedSize.substring(0, selectedSize.indexOf('x')));
            GameOptions gameOptions = new GameOptions(boardSize, thinkingTimeSeconds(thinkTimeBox.getSelectedIndex()),
                    randomEdgesBox.isSelected());
            return new HostSettings(gameOptions, selectedAddress.address(), port);
        }
    }

    private static void askLocalForSettingsChange(JFrame frame, SquaresPanel panel) {
        panel.setClockPausedByDialog(true);

        try {
            GameOptions gameOptions = askGameOptions(frame, panel.boardRows(), panel.thinkingTimeLimitSeconds(),
                    panel.randomInitialEdgesEnabled());

            if (gameOptions == null) {
                return;
            }

            int boardSize = gameOptions.boardSize();
            int choice = JOptionPane.showConfirmDialog(frame,
                    Messages.settingsRestartConfirm(),
                    Messages.CHANGE_SIZE_TITLE,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                panel.setThinkingTimeLimitSeconds(gameOptions.thinkingTimeSeconds());
                panel.setRandomInitialEdgesEnabled(gameOptions.randomEdges());
                panel.resizeBoard(boardSize, boardSize);
                panel.setNetworkInfo(Messages.localInfo(boardSize, boardSize));
                fitWindowToContent(frame);
            }
        } finally {
            panel.setClockPausedByDialog(false);
        }
    }

    static void fitWindowToContent(JFrame frame) {
        frame.setMinimumSize(null);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
    }

    static void showNetworkContent(JFrame frame, SquaresPanel panel, ChatPanel chatPanel) {
        JPanel content = new JPanel(new BorderLayout(10, 0));

        content.add(panel, BorderLayout.CENTER);
        content.add(chatPanel, BorderLayout.EAST);
        frame.setContentPane(content);
        fitWindowToContent(frame);
    }

    static void installWindowPauseHandling(JFrame frame, SquaresPanel panel) {
        Object existing = frame.getRootPane().getClientProperty(CLOCK_PAUSE_LISTENER_PROPERTY);

        if (existing instanceof WindowFocusListener) {
            frame.removeWindowFocusListener((WindowFocusListener) existing);
        }

        WindowFocusListener listener = new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                panel.setWindowActive(true);
            }

            @Override
            public void windowLostFocus(WindowEvent event) {
                panel.setWindowActive(false);
            }
        };

        frame.addWindowFocusListener(listener);
        frame.getRootPane().putClientProperty(CLOCK_PAUSE_LISTENER_PROPERTY, listener);
        panel.setWindowActive(frame.isActive());
    }

    private static void exitApplication(JFrame frame) {
        frame.dispose();
        System.exit(0);
    }

    private static void showAbout(JFrame frame, SquaresPanel panel, boolean pauseClock) {
        if (pauseClock) {
            panel.setClockPausedByDialog(true);
        }

        try {
            JOptionPane.showMessageDialog(frame,
                    Messages.aboutText(),
                    Messages.ABOUT_TITLE,
                    JOptionPane.INFORMATION_MESSAGE);
        } finally {
            if (pauseClock) {
                panel.setClockPausedByDialog(false);
            }
        }
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

    private static NetworkAddress askNetworkAddress(JFrame frame) {
        List<NetworkAddress> addresses = localIpAddresses(false);

        if (addresses.isEmpty()) {
            return null;
        }

        if (addresses.size() == 1) {
            return addresses.get(0);
        }

        NetworkAddress selected = (NetworkAddress) JOptionPane.showInputDialog(frame,
                Messages.ADAPTER_PROMPT,
                Messages.ADAPTER_TITLE,
                JOptionPane.PLAIN_MESSAGE,
                null,
                addresses.toArray(),
                addresses.get(0));

        return selected;
    }

    private static List<NetworkAddress> localIpAddresses(boolean includeVirtual) {
        List<NetworkAddress> addresses = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (!networkInterface.isUp() || networkInterface.isLoopback()
                        || (!includeVirtual && isVirtualLikeNetworkInterface(networkInterface))) {
                    continue;
                }

                Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();

                while (interfaceAddresses.hasMoreElements()) {
                    InetAddress address = interfaceAddresses.nextElement();
                    String value = address.getHostAddress();

                    if (!address.isLoopbackAddress() && value.indexOf(':') < 0) {
                        String label = networkInterface.getDisplayName();

                        addresses.add(new NetworkAddress(value, label));
                    }
                }
            }
        } catch (SocketException exception) {
            return addresses;
        }

        return addresses;
    }

    private static void selectAddress(JComboBox<NetworkAddress> adapterBox, String address) {
        for (int index = 0; index < adapterBox.getItemCount(); index++) {
            NetworkAddress item = adapterBox.getItemAt(index);
            if (item.address().equals(address)) {
                adapterBox.setSelectedIndex(index);
                return;
            }
        }
    }

    private static boolean hasAddress(List<NetworkAddress> addresses, String address) {
        for (NetworkAddress item : addresses) {
            if (item.address().equals(address)) {
                return true;
            }
        }

        return false;
    }

    private static Integer parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535 ? port : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isVirtualLikeNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        if (networkInterface.isVirtual()) {
            return true;
        }

        String name = (networkInterface.getName() + " " + networkInterface.getDisplayName()).toLowerCase();
        return name.contains("virtual")
                || name.contains("vmware")
                || name.contains("virtualbox")
                || name.contains("hyper-v")
                || name.contains("vbox")
                || name.contains("vmnet")
                || name.contains("wsl")
                || name.contains("docker")
                || name.contains("tap")
                || name.contains("tun")
                || name.contains("vpn")
                || name.contains("loopback");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int thinkingTimeSeconds(int index) {
        return index >= 0 && index < THINK_TIME_SECONDS.length ? THINK_TIME_SECONDS[index] : 0;
    }

    private static int thinkTimeIndex(int seconds) {
        for (int index = 0; index < THINK_TIME_SECONDS.length; index++) {
            if (THINK_TIME_SECONDS[index] == seconds) {
                return index;
            }
        }

        return 0;
    }

    private static final class HostSettings {
        private final GameOptions gameOptions;
        private final String hostAddress;
        private final int port;

        private HostSettings(GameOptions gameOptions, String hostAddress, int port) {
            this.gameOptions = gameOptions;
            this.hostAddress = hostAddress;
            this.port = port;
        }

        private GameOptions gameOptions() {
            return gameOptions;
        }

        private String hostAddress() {
            return hostAddress;
        }

        private int port() {
            return port;
        }

        private boolean networkChanged(String currentHostAddress, int currentPort) {
            return !hostAddress.equals(currentHostAddress) || port != currentPort;
        }
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

    private static final class GameOptions {
        private final int boardSize;
        private final int thinkingTimeSeconds;
        private final boolean randomEdges;

        private GameOptions(int boardSize, int thinkingTimeSeconds, boolean randomEdges) {
            this.boardSize = boardSize;
            this.thinkingTimeSeconds = thinkingTimeSeconds;
            this.randomEdges = randomEdges;
        }

        private int boardSize() {
            return boardSize;
        }

        private int thinkingTimeSeconds() {
            return thinkingTimeSeconds;
        }

        private boolean randomEdges() {
            return randomEdges;
        }
    }
}
