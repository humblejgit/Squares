package cz.humblej.squares.ui;

import cz.humblej.squares.model.LocalProfileStatistics;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.squares.auth.LeaderboardEntry;
import cz.humblej.squares.auth.LeaderboardPage;
import cz.humblej.squares.auth.LeaderboardStatistics;
import cz.humblej.squares.auth.OnlinePlayer;

import org.junit.Test;

import javax.swing.table.TableModel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StatisticsDialogTest {
    @Test
    public void leaderboardTableShowsRankingAndCannotBeEdited() {
        PlayerProfile active = new PlayerProfile(UUID.randomUUID(), "Jana", Instant.EPOCH, false);
        PlayerProfile archived = new PlayerProfile(UUID.randomUUID(), "Petr", Instant.EPOCH, true);
        TableModel model = StatisticsDialog.createTableModel(Arrays.asList(
                new LocalProfileStatistics(active, 2, 1, 1, 0, 20),
                new LocalProfileStatistics(archived, 1, 0, 0, 1, 5)));

        assertEquals(2, model.getRowCount());
        assertEquals(Messages.STATISTICS_COLUMN_PROFILE, model.getColumnName(1));
        assertEquals(1, model.getValueAt(0, 0));
        assertEquals("Jana", model.getValueAt(0, 1));
        assertEquals("50,0 %", model.getValueAt(0, 7));
        assertEquals("Petr (archivovan\u00fd)", model.getValueAt(1, 1));
        assertFalse(model.isCellEditable(0, 1));
    }

    @Test
    public void globalLeaderboardLoadsAsynchronouslyAndShowsLoggedOutState()
            throws Exception {
        FakeLeaderboardClient client = new FakeLeaderboardClient(false, populatedPage(), null);
        final GlobalLeaderboardPanel[] holder = new GlobalLeaderboardPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new GlobalLeaderboardPanel(client);
            holder[0].loadInitial();
        });

        assertTrue(client.started.await(2, TimeUnit.SECONDS));
        assertEquals(GlobalLeaderboardPanel.State.LOADING, holder[0].state());
        client.release.countDown();
        waitForState(holder[0], GlobalLeaderboardPanel.State.CONTENT);

        assertEquals(1, holder[0].table().getRowCount());
        assertEquals(Messages.STATISTICS_GLOBAL_LOGGED_OUT,
                ((JLabel) holder[0].myPosition().getComponent(0)).getText());
    }

    @Test
    public void globalLeaderboardShowsSignedInPlayerWithoutResults() throws Exception {
        FakeLeaderboardClient client = new FakeLeaderboardClient(true, populatedPage(), null);
        client.release.countDown();
        GlobalLeaderboardPanel panel = createAndLoad(client);

        waitForState(panel, GlobalLeaderboardPanel.State.CONTENT);

        assertEquals(Messages.STATISTICS_GLOBAL_NO_RESULT,
                ((JLabel) panel.myPosition().getComponent(0)).getText());
    }

    @Test
    public void globalLeaderboardShowsEmptyAndUnavailableStates() throws Exception {
        FakeLeaderboardClient empty = new FakeLeaderboardClient(
                false, new LeaderboardPage(Collections.<LeaderboardEntry>emptyList(),
                null, Instant.EPOCH), null);
        empty.release.countDown();
        GlobalLeaderboardPanel emptyPanel = createAndLoad(empty);
        waitForState(emptyPanel, GlobalLeaderboardPanel.State.EMPTY);

        FakeLeaderboardClient failed = new FakeLeaderboardClient(
                false, null, new AuthenticationException("server offline"));
        failed.release.countDown();
        GlobalLeaderboardPanel failedPanel = createAndLoad(failed);
        waitForState(failedPanel, GlobalLeaderboardPanel.State.ERROR);
    }

    @Test
    public void globalTableUsesServerRankAndHandle() {
        TableModel model = GlobalLeaderboardPanel.createTableModel(
                populatedPage().entries());

        assertEquals(128L, model.getValueAt(0, 0));
        assertEquals("BananaPlayer (@banan)", model.getValueAt(0, 1));
        assertEquals(386L, model.getValueAt(0, 6));
        assertEquals("57,1 %", model.getValueAt(0, 7));
        assertFalse(model.isCellEditable(0, 0));
    }

    private static GlobalLeaderboardPanel createAndLoad(FakeLeaderboardClient client)
            throws Exception {
        final GlobalLeaderboardPanel[] holder = new GlobalLeaderboardPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new GlobalLeaderboardPanel(client);
            holder[0].loadInitial();
        });
        return holder[0];
    }

    private static void waitForState(
            GlobalLeaderboardPanel panel, GlobalLeaderboardPanel.State expected)
            throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            SwingUtilities.invokeAndWait(() -> { });
            if (panel.state() == expected) {
                return;
            }
            Thread.sleep(20);
        }
        fail("Expected leaderboard state " + expected + " but was " + panel.state());
    }

    private static LeaderboardPage populatedPage() {
        OnlinePlayer player = new OnlinePlayer(
                "00000000-0000-0000-0000-000000000001",
                "banan", "BananaPlayer", 1);
        LeaderboardStatistics statistics = new LeaderboardStatistics(
                42, 24, 3, 15, 386, 57.142857);
        return new LeaderboardPage(Collections.singletonList(
                new LeaderboardEntry(128, player, statistics)),
                null, Instant.parse("2026-08-05T18:00:00Z"));
    }

    private static final class FakeLeaderboardClient implements LeaderboardClient {
        private final boolean session;
        private final LeaderboardPage page;
        private final AuthenticationException failure;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private FakeLeaderboardClient(
                boolean session, LeaderboardPage page, AuthenticationException failure) {
            this.session = session;
            this.page = page;
            this.failure = failure;
        }

        @Override
        public boolean hasSession() {
            return session;
        }

        @Override
        public LeaderboardPage getPage(int limit, String cursor)
                throws AuthenticationException {
            started.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    throw new AuthenticationException("test timeout");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AuthenticationException("test interrupted", exception);
            }
            if (failure != null) {
                throw failure;
            }
            return page;
        }

        @Override
        public LeaderboardEntry getMyEntry() {
            return null;
        }
    }
}
