package cz.humblej.squares.app;

import cz.humblej.squares.network.NetworkGame;
import cz.humblej.squares.ui.Messages;
import cz.humblej.squares.ui.SquaresPanel;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

final class GameOptionsDialog {
    private static final int[] THINK_TIME_SECONDS = {0, 30, 60, 120, 300};
    private static final String[] BOARD_SIZES = {"5x5", "6x6", "7x7", "8x8", "9x9", "10x10"};

    private GameOptionsDialog() {
    }

    static GameOptions ask(JFrame frame, int selectedBoardSize, int selectedThinkingTimeSeconds,
                           boolean selectedRandomEdges, boolean showDifficulty,
                           SquaresPanel.ComputerDifficulty selectedDifficulty) {
        String[] thinkTimes = {Messages.THINK_TIME_NONE, "30 s", "1 min", "2 min", "5 min"};
        JComboBox<String> sizeBox = new JComboBox<>(BOARD_SIZES);
        JComboBox<String> thinkTimeBox = new JComboBox<>(thinkTimes);
        JComboBox<SquaresPanel.ComputerDifficulty> difficultyBox =
                new JComboBox<>(SquaresPanel.ComputerDifficulty.values());
        JCheckBox randomEdgesBox = new JCheckBox(Messages.GAME_OPTIONS_RANDOM_EDGES);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = constraints();

        sizeBox.setSelectedItem(selectedBoardSize + "x" + selectedBoardSize);
        thinkTimeBox.setSelectedIndex(thinkTimeIndex(selectedThinkingTimeSeconds));
        difficultyBox.setSelectedItem(selectedDifficulty == null
                ? SquaresPanel.ComputerDifficulty.MEDIUM : selectedDifficulty);
        randomEdgesBox.setSelected(selectedRandomEdges);

        addRow(panel, constraints, 0, Messages.GAME_OPTIONS_BOARD_SIZE, sizeBox);
        addRow(panel, constraints, 1, Messages.GAME_OPTIONS_THINK_TIME, thinkTimeBox);

        int row = 2;
        if (showDifficulty) {
            addRow(panel, constraints, row++, Messages.GAME_OPTIONS_DIFFICULTY, difficultyBox);
        }

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(randomEdgesBox, constraints);

        int choice = JOptionPane.showConfirmDialog(frame, panel, Messages.GAME_OPTIONS_TITLE,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        SquaresPanel.ComputerDifficulty difficulty =
                (SquaresPanel.ComputerDifficulty) difficultyBox.getSelectedItem();
        return new GameOptions(selectedBoardSize(sizeBox), thinkingTimeSeconds(thinkTimeBox.getSelectedIndex()),
                randomEdgesBox.isSelected(),
                difficulty == null ? SquaresPanel.ComputerDifficulty.MEDIUM : difficulty);
    }

    static HostSettings askHost(JFrame frame, NetworkGame.HostController hostController) {
        String[] thinkTimes = {Messages.THINK_TIME_NONE, "30 s", "1 min", "2 min", "5 min"};
        JComboBox<String> sizeBox = new JComboBox<>(BOARD_SIZES);
        JComboBox<String> thinkTimeBox = new JComboBox<>(thinkTimes);
        JCheckBox randomEdgesBox = new JCheckBox(Messages.GAME_OPTIONS_RANDOM_EDGES);
        List<NetworkAddress> addresses = NetworkAddressSupport.localAddresses(true);
        if (!NetworkAddressSupport.contains(addresses, hostController.hostAddress())) {
            addresses.add(0, new NetworkAddress(hostController.hostAddress(), Messages.CURRENT_NETWORK_ADDRESS));
        }
        JComboBox<NetworkAddress> adapterBox = new JComboBox<>(addresses.toArray(new NetworkAddress[0]));
        JTextField portField = new JTextField(String.valueOf(hostController.port()), 8);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = constraints();

        sizeBox.setSelectedItem(hostController.boardSize() + "x" + hostController.boardSize());
        thinkTimeBox.setSelectedIndex(thinkTimeIndex(hostController.thinkingTimeLimitSeconds()));
        randomEdgesBox.setSelected(hostController.randomInitialEdgesEnabled());
        NetworkAddressSupport.select(adapterBox, hostController.hostAddress());
        adapterBox.setEnabled(hostController.canChangeNetworkEndpoint());
        portField.setEnabled(hostController.canChangeNetworkEndpoint());

        addRow(panel, constraints, 0, Messages.GAME_OPTIONS_BOARD_SIZE, sizeBox);
        addRow(panel, constraints, 1, Messages.GAME_OPTIONS_THINK_TIME, thinkTimeBox);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(randomEdgesBox, constraints);
        constraints.gridwidth = 1;
        addRow(panel, constraints, 3, Messages.NETWORK_SETTINGS_ADAPTER, adapterBox);
        addRow(panel, constraints, 4, Messages.NETWORK_SETTINGS_PORT, portField);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(frame, panel, Messages.GAME_OPTIONS_TITLE,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }

            Integer port = NetworkAddressSupport.parsePort(portField.getText());
            if (port == null) {
                JOptionPane.showMessageDialog(frame, Messages.INVALID_PORT,
                        Messages.NETWORK_GAME_TITLE, JOptionPane.WARNING_MESSAGE);
                continue;
            }

            NetworkAddress address = (NetworkAddress) adapterBox.getSelectedItem();
            if (address == null) {
                JOptionPane.showMessageDialog(frame, Messages.NO_NETWORK_ADAPTER,
                        Messages.ADAPTER_TITLE, JOptionPane.WARNING_MESSAGE);
                continue;
            }

            GameOptions options = new GameOptions(selectedBoardSize(sizeBox),
                    thinkingTimeSeconds(thinkTimeBox.getSelectedIndex()), randomEdgesBox.isSelected(),
                    SquaresPanel.ComputerDifficulty.MEDIUM);
            return new HostSettings(options, address.address(), port);
        }
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints, int row,
                               String label, java.awt.Component component) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, constraints);
    }

    private static int selectedBoardSize(JComboBox<String> sizeBox) {
        String selected = (String) sizeBox.getSelectedItem();
        return selected == null ? 5 : Integer.parseInt(selected.substring(0, selected.indexOf('x')));
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
}
