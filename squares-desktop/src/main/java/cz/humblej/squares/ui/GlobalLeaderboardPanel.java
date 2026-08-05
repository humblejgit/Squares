package cz.humblej.squares.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.squares.auth.LeaderboardEntry;
import cz.humblej.squares.auth.LeaderboardPage;
import cz.humblej.squares.auth.LeaderboardStatistics;

final class GlobalLeaderboardPanel extends JPanel {
    enum State { IDLE, LOADING, CONTENT, EMPTY, ERROR }

    private static final int PAGE_SIZE = 50;
    private static final String LOADING_CARD = "loading";
    private static final String CONTENT_CARD = "content";
    private static final String EMPTY_CARD = "empty";
    private static final String ERROR_CARD = "error";

    private final LeaderboardClient client;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JPanel myPosition = new JPanel(new GridLayout(0, 1, 0, 3));
    private final JTable table = new JTable();
    private final JLabel updated = new JLabel();
    private final JLabel errorDetail = new JLabel(" ");
    private final JButton previous = new JButton(Messages.STATISTICS_GLOBAL_PREVIOUS);
    private final JButton next = new JButton(Messages.STATISTICS_GLOBAL_NEXT);
    private final Deque<String> previousCursors = new ArrayDeque<String>();

    private String currentCursor;
    private String nextCursor;
    private boolean started;
    private int generation;
    private State state = State.IDLE;

    GlobalLeaderboardPanel(LeaderboardClient client) {
        super(new BorderLayout());
        this.client = client;
        setPreferredSize(new Dimension(780, 350));
        cardPanel.add(centered(Messages.STATISTICS_GLOBAL_LOADING), LOADING_CARD);
        cardPanel.add(createContentPanel(), CONTENT_CARD);
        cardPanel.add(createEmptyPanel(), EMPTY_CARD);
        cardPanel.add(createErrorPanel(), ERROR_CARD);
        add(cardPanel, BorderLayout.CENTER);
    }

    void loadInitial() {
        if (!started) {
            started = true;
            currentCursor = null;
            previousCursors.clear();
            loadCurrentPage();
        }
    }

    State state() {
        return state;
    }

    JTable table() {
        return table;
    }

    JPanel myPosition() {
        return myPosition;
    }

    private JPanel createContentPanel() {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        myPosition.setBorder(BorderFactory.createTitledBorder(
                Messages.STATISTICS_GLOBAL_MY_POSITION));
        content.add(myPosition, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(updated, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton refresh = new JButton(Messages.STATISTICS_GLOBAL_REFRESH);
        refresh.addActionListener(event -> refresh());
        previous.addActionListener(event -> previousPage());
        next.addActionListener(event -> nextPage());
        actions.add(refresh);
        actions.add(previous);
        actions.add(next);
        footer.add(actions, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    private JPanel createEmptyPanel() {
        JPanel empty = centered(Messages.STATISTICS_GLOBAL_EMPTY);
        JButton refresh = new JButton(Messages.STATISTICS_GLOBAL_REFRESH);
        refresh.addActionListener(event -> refresh());
        empty.add(refresh, BorderLayout.SOUTH);
        return empty;
    }

    private JPanel createErrorPanel() {
        JPanel error = centered(Messages.STATISTICS_GLOBAL_UNAVAILABLE);
        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 6));
        errorDetail.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(errorDetail);
        JButton retry = new JButton(Messages.STATISTICS_GLOBAL_RETRY);
        retry.addActionListener(event -> loadCurrentPage());
        JPanel action = new JPanel();
        action.add(retry);
        bottom.add(action);
        error.add(bottom, BorderLayout.SOUTH);
        return error;
    }

    private static JPanel centered(String message) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(35, 15, 35, 15));
        panel.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }

    private void refresh() {
        previousCursors.clear();
        currentCursor = null;
        loadCurrentPage();
    }

    private void previousPage() {
        if (previousCursors.isEmpty()) {
            return;
        }
        String cursor = previousCursors.pop();
        currentCursor = cursor.isEmpty() ? null : cursor;
        loadCurrentPage();
    }

    private void nextPage() {
        if (nextCursor == null) {
            return;
        }
        previousCursors.push(currentCursor == null ? "" : currentCursor);
        currentCursor = nextCursor;
        loadCurrentPage();
    }

    private void loadCurrentPage() {
        final int requestGeneration = ++generation;
        final String requestedCursor = currentCursor;
        showState(State.LOADING);
        new SwingWorker<LoadResult, Void>() {
            @Override
            protected LoadResult doInBackground() throws Exception {
                LeaderboardPage page = client.getPage(PAGE_SIZE, requestedCursor);
                boolean signedIn = client.hasSession();
                LeaderboardEntry myEntry = null;
                if (signedIn) {
                    try {
                        myEntry = client.getMyEntry();
                    } catch (AuthenticationException exception) {
                        if (exception.sessionExpired()) {
                            signedIn = false;
                        } else {
                            throw exception;
                        }
                    }
                }
                return new LoadResult(page, signedIn, myEntry);
            }

            @Override
            protected void done() {
                if (requestGeneration != generation) {
                    return;
                }
                try {
                    display(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    displayError(exception);
                } catch (ExecutionException exception) {
                    displayError(exception.getCause());
                }
            }
        }.execute();
    }

    private void display(LoadResult result) {
        LeaderboardPage page = result.page;
        if (page.entries().isEmpty()) {
            nextCursor = null;
            showState(State.EMPTY);
            return;
        }
        table.setModel(createTableModel(page.entries()));
        nextCursor = page.nextCursor();
        previous.setEnabled(!previousCursors.isEmpty());
        next.setEnabled(nextCursor != null);
        updated.setText(Messages.statisticsGlobalUpdated(page.generatedAt()));
        displayMyPosition(result.signedIn, result.myEntry);
        selectMyRow(result.myEntry, page.entries());
        showState(State.CONTENT);
    }

    private void displayMyPosition(boolean signedIn, LeaderboardEntry entry) {
        myPosition.removeAll();
        if (!signedIn) {
            myPosition.add(new JLabel(Messages.STATISTICS_GLOBAL_LOGGED_OUT));
        } else if (entry == null) {
            myPosition.add(new JLabel(Messages.STATISTICS_GLOBAL_NO_RESULT));
        } else {
            LeaderboardStatistics statistics = entry.statistics();
            myPosition.add(new JLabel(Messages.statisticsGlobalRank(entry.rank()) + "  "
                    + Messages.statisticsGlobalPlayer(
                    entry.player().displayName(), entry.player().handle())));
            myPosition.add(new JLabel(Messages.statisticsRecord(
                    statistics.games(), statistics.wins(),
                    statistics.draws(), statistics.losses())));
            myPosition.add(new JLabel(Messages.statisticsScore(
                    statistics.totalScore(), statistics.winPercentage())));
        }
        myPosition.revalidate();
        myPosition.repaint();
    }

    private void selectMyRow(LeaderboardEntry mine, List<LeaderboardEntry> entries) {
        table.clearSelection();
        if (mine == null) {
            return;
        }
        for (int index = 0; index < entries.size(); index++) {
            if (mine.player().playerId().equals(entries.get(index).player().playerId())) {
                table.setRowSelectionInterval(index, index);
                return;
            }
        }
    }

    private void displayError(Throwable error) {
        String message = error == null ? null : error.getMessage();
        errorDetail.setText(message == null || message.trim().isEmpty() ? " " : message);
        showState(State.ERROR);
    }

    private void showState(State newState) {
        state = newState;
        String card = newState == State.CONTENT ? CONTENT_CARD
                : newState == State.EMPTY ? EMPTY_CARD
                : newState == State.ERROR ? ERROR_CARD : LOADING_CARD;
        cards.show(cardPanel, card);
    }

    static TableModel createTableModel(List<LeaderboardEntry> entries) {
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
        Object[][] rows = new Object[entries.size()][columns.length];
        for (int index = 0; index < entries.size(); index++) {
            LeaderboardEntry entry = entries.get(index);
            LeaderboardStatistics statistics = entry.statistics();
            rows[index][0] = entry.rank();
            rows[index][1] = Messages.statisticsGlobalPlayer(
                    entry.player().displayName(), entry.player().handle());
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

    private static final class LoadResult {
        private final LeaderboardPage page;
        private final boolean signedIn;
        private final LeaderboardEntry myEntry;

        private LoadResult(
                LeaderboardPage page, boolean signedIn, LeaderboardEntry myEntry) {
            this.page = page;
            this.signedIn = signedIn;
            this.myEntry = myEntry;
        }
    }
}
