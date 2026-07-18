package cz.codex.squares;

import javax.swing.JFrame;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridLayout;
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
        LocalDatabase database = LocalDatabase.applicationDatabase();
        try {
            database.initialize();
        } catch (StorageException exception) {
            JOptionPane.showMessageDialog(null,
                    Messages.databaseInitializationFailed(exception.getMessage()),
                    Messages.DATABASE_ERROR_TITLE,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ProfileStore profileStore = new SqliteProfileStore(database);
        GameStore gameStore = new SqliteGameStore(database);
        StatisticsStore statisticsStore = new SqliteStatisticsStore(database);
        PlayerProfile activeProfile = ProfileDialog.chooseActive(null, profileStore);
        if (activeProfile == null) {
            return;
        }

        int gameMode = askGameMode();

        if (gameMode < 0) {
            return;
        }

        if (gameMode == 3) {
            showClientWindow(activeProfile, gameStore, statisticsStore);
            return;
        }

        GameOptions gameOptions = askGameOptions(null, 8, 0, false,
                gameMode == 1, SquaresPanel.ComputerDifficulty.MEDIUM);

        if (gameOptions == null) {
            return;
        }

        int boardSize = gameOptions.boardSize();
        PlayerProfile opponentProfile = gameMode == 0
                ? ProfileDialog.chooseOpponent(null, profileStore, activeProfile)
                : null;
        JFrame frame = new JFrame(Messages.APP_TITLE);
        SquaresPanel panel = new SquaresPanel(boardSize, boardSize);
        panel.setPlayerProfiles(activeProfile, opponentProfile);
        panel.setThinkingTimeLimitSeconds(gameOptions.thinkingTimeSeconds());
        panel.setRandomInitialEdgesEnabled(gameOptions.randomEdges());
        if (gameMode == 1) {
            panel.setComputerOpponent(gameOptions.computerDifficulty());
        }
        panel.resetGame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        GameResultRecorder recorder = new GameResultRecorder(frame, gameStore);
        if (!configureGameMode(frame, panel, boardSize, gameMode, activeProfile,
                profileStore, statisticsStore, recorder)) {
            frame.dispose();
            return;
        }
        fitWindowToContent(frame);
        frame.setVisible(true);
    }

    private static void showClientWindow(PlayerProfile activeProfile, GameStore gameStore,
                                         StatisticsStore statisticsStore) {
        String host = JOptionPane.showInputDialog(null, Messages.HOST_ADDRESS_PROMPT, "127.0.0.1");

        if (isBlank(host)) {
            return;
        }

        int port = DEFAULT_NETWORK_PORT;
        JFrame frame = new JFrame(profileWindowTitle(Messages.WINDOW_CLIENT, activeProfile));
        JLabel connectingLabel = new JLabel(Messages.CONNECTING_TO_HOST, SwingConstants.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(connectingLabel);
        frame.setSize(360, 140);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        NetworkGame.join(frame, host.trim(), port, activeProfile,
                new GameResultRecorder(frame, gameStore), statisticsStore);
    }

    private static int askGameMode() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));
        JDialog dialog = new JDialog((JFrame) null, Messages.APP_TITLE, true);
        int[] selectedMode = {-1};

        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel(Messages.GAME_MODE_PROMPT, JLabel.CENTER), BorderLayout.NORTH);
        addGameModeButton(buttons, dialog, selectedMode, Messages.GAME_MODE_COMPUTER, 1);
        addGameModeButton(buttons, dialog, selectedMode, Messages.GAME_MODE_LOCAL, 0);
        addGameModeButton(buttons, dialog, selectedMode, Messages.GAME_MODE_HOST, 2);
        addGameModeButton(buttons, dialog, selectedMode, Messages.GAME_MODE_JOIN, 3);
        panel.add(buttons, BorderLayout.CENTER);

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return selectedMode[0];
    }

    private static void addGameModeButton(JPanel panel, JDialog dialog, int[] selectedMode, String label, int mode) {
        JButton button = new JButton(label);

        button.setFocusable(false);
        button.addActionListener(event -> {
            selectedMode[0] = mode;
            dialog.dispose();
        });
        panel.add(button);
    }

    private static boolean configureGameMode(JFrame frame, SquaresPanel panel, int boardSize, int gameMode,
                                             PlayerProfile activeProfile, ProfileStore profileStore,
                                             StatisticsStore statisticsStore,
                                             GameResultRecorder recorder) {
        if (gameMode == 2) {
            panel.setClockEnabled(false);
            panel.resetClock();
            NetworkAddress networkAddress = askNetworkAddress(frame);
            if (networkAddress == null) {
                JOptionPane.showMessageDialog(frame, Messages.NO_NETWORK_ADAPTER,
                        Messages.ADAPTER_TITLE, JOptionPane.WARNING_MESSAGE);
                return false;
            }

            int port = DEFAULT_NETWORK_PORT;
            frame.setTitle(profileWindowTitle(Messages.WINDOW_HOST, activeProfile));
            panel.setNetworkInfo(Messages.hostInfo(networkAddress.address(), port, boardSize, boardSize));
            NetworkGame.HostController hostController = NetworkGame.host(frame, panel, networkAddress.address(), port,
                    activeProfile, recorder);
            installGameMenu(frame, panel, false,
                    () -> askHostForSettingsChange(frame, hostController),
                    () -> showNetworkProfileChangeMessage(frame),
                    () -> showStatistics(frame, panel, statisticsStore, activeProfile, false));
            return true;
        } else {
            if (gameMode == 1) {
                panel.setComputerOpponent(panel.computerDifficulty());
                panel.setNetworkInfo(Messages.computerInfo(boardSize, boardSize,
                        panel.redPlayerDisplayName(),
                        panel.computerDifficulty().toString()));
            } else {
                panel.clearComputerOpponent();
                panel.setLocalPlayer(SquaresPanel.NO_PLAYER);
                panel.setNetworkInfo(Messages.localInfo(boardSize, boardSize,
                        panel.redPlayerDisplayName(), panel.bluePlayerDisplayName()));
            }
            panel.setGameOverHandler(result -> {
                recorder.record(result);
                askForNewLocalGame(frame, panel, result);
            });
            panel.setRestartHandler(() -> askForLocalRestart(frame, panel));
            installWindowPauseHandling(frame, panel);
            installGameMenu(frame, panel, true,
                    () -> askLocalForSettingsChange(frame, panel, gameMode == 1),
                    () -> askLocalForProfileSwitch(frame, panel, profileStore, gameMode == 1),
                    () -> showStatistics(frame, panel, statisticsStore, panel.redPlayerProfile(), true));
            frame.setTitle(profileWindowTitle(
                    gameMode == 1 ? Messages.WINDOW_COMPUTER : Messages.WINDOW_LOCAL,
                    activeProfile));
            return true;
        }
    }

    private static String profileWindowTitle(String title, PlayerProfile profile) {
        return title + " - " + profile.displayName();
    }

    private static void askLocalForProfileSwitch(JFrame frame, SquaresPanel panel,
                                                  ProfileStore profileStore, boolean computerOpponent) {
        panel.setClockPausedByDialog(true);

        try {
            PlayerProfile previousRed = panel.redPlayerProfile();
            PlayerProfile previousBlue = panel.bluePlayerProfile();
            PlayerProfile selected = ProfileDialog.chooseActive(frame, profileStore);

            if (selected == null) {
                return;
            }

            List<PlayerProfile> activeProfiles;
            try {
                activeProfiles = profileStore.findActive();
            } catch (StorageException exception) {
                JOptionPane.showMessageDialog(frame, exception.getMessage(),
                        Messages.PROFILE_TITLE, JOptionPane.WARNING_MESSAGE);
                return;
            }

            PlayerProfile refreshedPreviousRed = findProfile(activeProfiles, previousRed);
            PlayerProfile nextBlue = computerOpponent ? null : findProfile(activeProfiles, previousBlue);

            if (nextBlue != null && selected.equals(nextBlue)) {
                nextBlue = refreshedPreviousRed != null && !selected.equals(refreshedPreviousRed)
                        ? refreshedPreviousRed
                        : null;
            }

            if (sameProfileSnapshot(previousRed, selected)
                    && sameProfileSnapshot(previousBlue, nextBlue)) {
                return;
            }

            panel.setPlayerProfiles(selected, nextBlue);
            int boardSize = panel.boardRows();
            if (computerOpponent) {
                panel.setNetworkInfo(Messages.computerInfo(boardSize, panel.boardColumns(),
                        panel.redPlayerDisplayName(), panel.computerDifficulty().toString()));
                frame.setTitle(profileWindowTitle(Messages.WINDOW_COMPUTER, selected));
            } else {
                panel.setNetworkInfo(Messages.localInfo(boardSize, panel.boardColumns(),
                        panel.redPlayerDisplayName(), panel.bluePlayerDisplayName()));
                frame.setTitle(profileWindowTitle(Messages.WINDOW_LOCAL, selected));
            }
            panel.resetGame();
        } finally {
            panel.setClockPausedByDialog(false);
        }
    }

    private static PlayerProfile findProfile(List<PlayerProfile> profiles, PlayerProfile original) {
        if (original == null) {
            return null;
        }

        for (PlayerProfile profile : profiles) {
            if (profile.equals(original)) {
                return profile;
            }
        }
        return null;
    }

    private static boolean sameProfileSnapshot(PlayerProfile first, PlayerProfile second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.equals(second) && first.displayName().equals(second.displayName());
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
                                              boolean selectedRandomEdges, boolean showDifficulty,
                                              SquaresPanel.ComputerDifficulty selectedDifficulty) {
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
        JComboBox<SquaresPanel.ComputerDifficulty> difficultyBox =
                new JComboBox<>(SquaresPanel.ComputerDifficulty.values());
        JCheckBox randomEdgesBox = new JCheckBox(Messages.GAME_OPTIONS_RANDOM_EDGES);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        sizeBox.setSelectedItem(selectedBoardSize + "x" + selectedBoardSize);
        thinkTimeBox.setSelectedIndex(thinkTimeIndex(selectedThinkingTimeSeconds));
        difficultyBox.setSelectedItem(selectedDifficulty == null
                ? SquaresPanel.ComputerDifficulty.MEDIUM
                : selectedDifficulty);
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

        int row = 2;
        if (showDifficulty) {
            constraints.gridx = 0;
            constraints.gridy = row;
            constraints.gridwidth = 1;
            constraints.fill = GridBagConstraints.NONE;
            panel.add(new JLabel(Messages.GAME_OPTIONS_DIFFICULTY), constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            panel.add(difficultyBox, constraints);
            row++;
        }

        constraints.gridx = 0;
        constraints.gridy = row;
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
        SquaresPanel.ComputerDifficulty difficulty =
                (SquaresPanel.ComputerDifficulty) difficultyBox.getSelectedItem();
        return new GameOptions(boardSize, thinkingTimeSeconds(thinkTimeBox.getSelectedIndex()),
                randomEdgesBox.isSelected(),
                difficulty == null ? SquaresPanel.ComputerDifficulty.MEDIUM : difficulty);
    }

    private static void installGameMenu(JFrame frame, SquaresPanel panel, boolean pauseClockForDialogs,
                                        Runnable changeSizeAction, Runnable switchProfileAction,
                                        Runnable statisticsAction) {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu(Messages.MENU_GAME);
        JMenuItem settingsItem = new JMenuItem(Messages.MENU_SETTINGS);
        JMenuItem switchProfileItem = new JMenuItem(Messages.MENU_SWITCH_PROFILE);
        JMenuItem statisticsItem = new JMenuItem(Messages.MENU_STATISTICS);
        JCheckBoxMenuItem soundsItem = new JCheckBoxMenuItem(Messages.MENU_SOUNDS, SoundPlayer.isEnabled());
        JMenuItem aboutItem = new JMenuItem(Messages.MENU_ABOUT);
        JMenuItem exitItem = new JMenuItem(Messages.MENU_EXIT);

        if (changeSizeAction != null) {
            settingsItem.addActionListener(event -> changeSizeAction.run());
        }
        switchProfileItem.addActionListener(event -> switchProfileAction.run());
        statisticsItem.addActionListener(event -> statisticsAction.run());
        soundsItem.addActionListener(event -> SoundPlayer.setEnabled(soundsItem.isSelected()));
        aboutItem.addActionListener(event -> showAbout(frame, panel, pauseClockForDialogs));
        exitItem.addActionListener(event -> exitApplication(frame));
        if (changeSizeAction != null) {
            gameMenu.add(settingsItem);
        }
        gameMenu.add(switchProfileItem);
        gameMenu.add(statisticsItem);
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

    static void installNetworkClientGameMenu(JFrame frame, SquaresPanel panel,
                                             StatisticsStore statisticsStore,
                                             PlayerProfile localProfile) {
        installGameMenu(frame, panel, false, null,
                () -> showNetworkProfileChangeMessage(frame),
                () -> showStatistics(frame, panel, statisticsStore, localProfile, false));
    }

    private static void showStatistics(JFrame frame, SquaresPanel panel,
                                       StatisticsStore statisticsStore,
                                       PlayerProfile currentProfile, boolean pauseClock) {
        if (pauseClock) {
            panel.setClockPausedByDialog(true);
        }

        try {
            StatisticsDialog.show(frame, statisticsStore, currentProfile);
        } finally {
            if (pauseClock) {
                panel.setClockPausedByDialog(false);
            }
        }
    }

    private static void showNetworkProfileChangeMessage(JFrame frame) {
        JOptionPane.showMessageDialog(frame,
                Messages.PROFILE_NETWORK_CHANGE_ONLY_AT_START,
                Messages.PROFILE_TITLE,
                JOptionPane.INFORMATION_MESSAGE);
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
                    randomEdgesBox.isSelected(), SquaresPanel.ComputerDifficulty.MEDIUM);
            return new HostSettings(gameOptions, selectedAddress.address(), port);
        }
    }

    private static void askLocalForSettingsChange(JFrame frame, SquaresPanel panel, boolean computerOpponent) {
        panel.setClockPausedByDialog(true);

        try {
            GameOptions gameOptions = askGameOptions(frame, panel.boardRows(), panel.thinkingTimeLimitSeconds(),
                    panel.randomInitialEdgesEnabled(), computerOpponent, panel.computerDifficulty());

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
                if (computerOpponent) {
                    panel.setComputerOpponent(gameOptions.computerDifficulty());
                }
                panel.resizeBoard(boardSize, boardSize);
                panel.setNetworkInfo(computerOpponent
                        ? Messages.computerInfo(boardSize, boardSize, panel.redPlayerDisplayName(),
                                gameOptions.computerDifficulty().toString())
                        : Messages.localInfo(boardSize, boardSize,
                                panel.redPlayerDisplayName(), panel.bluePlayerDisplayName()));
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

    private static void askForNewLocalGame(JFrame frame, SquaresPanel panel, GameResult result) {
        String message = Messages.gameOver(result);
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
        private final SquaresPanel.ComputerDifficulty computerDifficulty;

        private GameOptions(int boardSize, int thinkingTimeSeconds, boolean randomEdges,
                            SquaresPanel.ComputerDifficulty computerDifficulty) {
            this.boardSize = boardSize;
            this.thinkingTimeSeconds = thinkingTimeSeconds;
            this.randomEdges = randomEdges;
            this.computerDifficulty = computerDifficulty;
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

        private SquaresPanel.ComputerDifficulty computerDifficulty() {
            return computerDifficulty;
        }
    }
}
