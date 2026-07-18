package cz.codex.squares;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

final class StatisticsDialog {
    private StatisticsDialog() {
    }

    static void show(JFrame owner, StatisticsStore store, PlayerProfile currentProfile) {
        List<LocalProfileStatistics> leaderboard;
        try {
            leaderboard = store.findLocalLeaderboard();
        } catch (StorageException exception) {
            JOptionPane.showMessageDialog(owner, exception.getMessage(),
                    Messages.STATISTICS_TITLE, JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(owner, createContent(leaderboard, currentProfile),
                Messages.STATISTICS_TITLE, JOptionPane.PLAIN_MESSAGE);
    }

    static JPanel createContent(List<LocalProfileStatistics> leaderboard, PlayerProfile currentProfile) {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(new EmptyBorder(4, 4, 4, 4));
        content.add(createCurrentProfilePanel(leaderboard, currentProfile), BorderLayout.NORTH);

        JTable table = new JTable(createTableModel(leaderboard));
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        int currentRow = findProfileRow(leaderboard, currentProfile);
        if (currentRow >= 0) {
            table.setRowSelectionInterval(currentRow, currentRow);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder(Messages.STATISTICS_LOCAL_LEADERBOARD));
        scrollPane.setPreferredSize(new Dimension(760, 280));
        content.add(scrollPane, BorderLayout.CENTER);
        return content;
    }

    static TableModel createTableModel(List<LocalProfileStatistics> leaderboard) {
        String[] columns = {
                Messages.STATISTICS_COLUMN_POSITION,
                Messages.STATISTICS_COLUMN_PROFILE,
                Messages.STATISTICS_COLUMN_GAMES,
                Messages.STATISTICS_COLUMN_WINS,
                Messages.STATISTICS_COLUMN_DRAWS,
                Messages.STATISTICS_COLUMN_LOSSES,
                Messages.STATISTICS_COLUMN_SCORE,
                Messages.STATISTICS_COLUMN_WIN_PERCENTAGE
        };
        Object[][] rows = new Object[leaderboard.size()][columns.length];

        for (int index = 0; index < leaderboard.size(); index++) {
            LocalProfileStatistics statistics = leaderboard.get(index);
            rows[index][0] = index + 1;
            rows[index][1] = Messages.statisticsProfileName(statistics.profile());
            rows[index][2] = statistics.games();
            rows[index][3] = statistics.wins();
            rows[index][4] = statistics.draws();
            rows[index][5] = statistics.losses();
            rows[index][6] = statistics.totalScore();
            rows[index][7] = Messages.formatWinPercentage(statistics.winPercentage());
        }

        return new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JPanel createCurrentProfilePanel(List<LocalProfileStatistics> leaderboard,
                                                    PlayerProfile currentProfile) {
        JPanel summary = new JPanel(new GridLayout(0, 1, 0, 4));
        summary.setBorder(BorderFactory.createTitledBorder(currentProfile == null
                ? Messages.STATISTICS_TITLE
                : Messages.statisticsCurrentProfile(currentProfile.displayName())));

        int row = findProfileRow(leaderboard, currentProfile);
        if (row < 0) {
            summary.add(new JLabel(Messages.STATISTICS_CURRENT_PROFILE_MISSING));
            return summary;
        }

        LocalProfileStatistics statistics = leaderboard.get(row);
        summary.add(new JLabel(Messages.statisticsRecord(statistics.games(), statistics.wins(),
                statistics.draws(), statistics.losses())));
        summary.add(new JLabel(Messages.statisticsScore(statistics.totalScore(), statistics.winPercentage())));
        return summary;
    }

    private static int findProfileRow(List<LocalProfileStatistics> leaderboard, PlayerProfile profile) {
        if (profile == null) {
            return -1;
        }

        for (int index = 0; index < leaderboard.size(); index++) {
            if (leaderboard.get(index).profile().equals(profile)) {
                return index;
            }
        }
        return -1;
    }
}
