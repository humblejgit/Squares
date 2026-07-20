package cz.humblej.squares.app;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.network.NetworkGame;
import cz.humblej.squares.persistence.GameResultRecorder;
import cz.humblej.squares.persistence.GameStore;
import cz.humblej.squares.persistence.LocalDatabase;
import cz.humblej.squares.persistence.ProfileStore;
import cz.humblej.squares.persistence.SqliteGameStore;
import cz.humblej.squares.persistence.SqliteProfileStore;
import cz.humblej.squares.persistence.SqliteStatisticsStore;
import cz.humblej.squares.persistence.StatisticsStore;
import cz.humblej.squares.persistence.StorageException;
import cz.humblej.squares.ui.ChatPanel;
import cz.humblej.squares.ui.Messages;
import cz.humblej.squares.ui.ProfileDialog;
import cz.humblej.squares.ui.SoundPlayer;
import cz.humblej.squares.ui.SquaresPanel;
import cz.humblej.squares.ui.StatisticsDialog;

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
import java.util.List;

public final class SquaresApp {
    private static final int DEFAULT_NETWORK_PORT = 1080;

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

        GameOptions gameOptions = GameOptionsDialog.ask(null, 8, 0, false,
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
        AppWindowSupport.fitToContent(frame);
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
            NetworkAddress networkAddress = NetworkAddressSupport.ask(frame);
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
            AppWindowSupport.installPauseHandling(frame, panel);
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
        AppWindowSupport.fitToContent(frame);
    }

    public static void installNetworkClientGameMenu(JFrame frame, SquaresPanel panel,
                                             StatisticsStore statisticsStore,
                                             PlayerProfile localProfile) {
        installGameMenu(frame, panel, false, null,
                () -> showNetworkProfileChangeMessage(frame),
                () -> showStatistics(frame, panel, statisticsStore, localProfile, false));
    }

    public static void fitWindowToContent(JFrame frame) {
        AppWindowSupport.fitToContent(frame);
    }

    public static void showNetworkContent(JFrame frame, SquaresPanel panel, ChatPanel chatPanel) {
        AppWindowSupport.showNetworkContent(frame, panel, chatPanel);
    }

    public static void installWindowPauseHandling(JFrame frame, SquaresPanel panel) {
        AppWindowSupport.installPauseHandling(frame, panel);
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
            HostSettings hostSettings = GameOptionsDialog.askHost(frame, hostController);

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
                AppWindowSupport.fitToContent(frame);
            }
        } finally {
            hostController.setSettingsDialogOpen(false);
        }
    }

    private static void askLocalForSettingsChange(JFrame frame, SquaresPanel panel, boolean computerOpponent) {
        panel.setClockPausedByDialog(true);

        try {
            GameOptions gameOptions = GameOptionsDialog.ask(frame, panel.boardRows(), panel.thinkingTimeLimitSeconds(),
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
                AppWindowSupport.fitToContent(frame);
            }
        } finally {
            panel.setClockPausedByDialog(false);
        }
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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
