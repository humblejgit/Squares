package cz.codex.squares;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SqliteStoresTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private LocalDatabase database;

    @Before
    public void initializeDatabase() throws Exception {
        database = new LocalDatabase(temporaryFolder.newFolder("data").toPath().resolve("squares.db"));
        database.initialize();
    }

    @Test
    public void profileCanBeCreatedSelectedRenamedAndArchived() throws Exception {
        ProfileStore store = new SqliteProfileStore(database);
        PlayerProfile jana = store.create("Jana");
        PlayerProfile petr = store.create("Petr");

        store.select(jana.id());
        assertEquals(jana, store.findSelected());

        store.rename(jana.id(), "Jana K.");
        assertEquals("Jana K.", store.findSelected().displayName());

        store.archive(jana.id());
        List<PlayerProfile> active = store.findActive();
        assertEquals(1, active.size());
        assertEquals(petr, active.get(0));
        assertNull(store.findSelected());
    }

    @Test(expected = StorageException.class)
    public void profileNamesAreUniqueIgnoringCase() throws Exception {
        ProfileStore store = new SqliteProfileStore(database);
        store.create("Jana");
        store.create("  JANA  ");
    }

    @Test
    public void gameAndOutboxEventAreCommittedOnce() throws Exception {
        GameStore store = new SqliteGameStore(database);
        GameResult result = networkResult();

        assertTrue(store.save(result));
        assertFalse(store.save(result));
        assertEquals(1L, store.countGames());
        assertEquals(1L, store.countPendingOutboxEvents());
    }

    @Test
    public void localLeaderboardIncludesArchivedProfilesAndExcludesRemoteProfiles() throws Exception {
        ProfileStore profiles = new SqliteProfileStore(database);
        PlayerProfile jana = profiles.create("Jana");
        PlayerProfile petr = profiles.create("Petr");
        PlayerProfile archived = profiles.create("Archiv");
        PlayerProfile adam = profiles.create("Adam");
        PlayerProfile zdena = profiles.create("Zdena");
        PlayerProfile remote = PlayerProfile.create("Vzdaleny");
        GameStore games = new SqliteGameStore(database);

        games.save(networkResult(jana, 10, PlayerResult.Outcome.WIN,
                remote, 2, PlayerResult.Outcome.LOSS));
        games.save(networkResult(jana, 10, PlayerResult.Outcome.DRAW,
                archived, 10, PlayerResult.Outcome.DRAW));
        games.save(networkResult(petr, 20, PlayerResult.Outcome.WIN,
                remote, 3, PlayerResult.Outcome.LOSS));
        games.save(networkResult(adam, 0, PlayerResult.Outcome.LOSS,
                remote, 5, PlayerResult.Outcome.WIN));
        games.save(networkResult(zdena, 0, PlayerResult.Outcome.LOSS,
                remote, 5, PlayerResult.Outcome.WIN));
        profiles.archive(archived.id());

        List<LocalProfileStatistics> leaderboard =
                new SqliteStatisticsStore(database).findLocalLeaderboard();

        assertEquals(5, leaderboard.size());
        assertEquals("Jana", leaderboard.get(0).profile().displayName());
        assertEquals(2L, leaderboard.get(0).games());
        assertEquals(1L, leaderboard.get(0).wins());
        assertEquals(1L, leaderboard.get(0).draws());
        assertEquals(0L, leaderboard.get(0).losses());
        assertEquals(20L, leaderboard.get(0).totalScore());
        assertEquals(50.0, leaderboard.get(0).winPercentage(), 0.001);

        assertEquals("Petr", leaderboard.get(1).profile().displayName());
        assertEquals(20L, leaderboard.get(1).totalScore());
        assertEquals(1L, leaderboard.get(1).games());

        assertEquals("Archiv", leaderboard.get(2).profile().displayName());
        assertTrue(leaderboard.get(2).profile().archived());
        assertEquals(1L, leaderboard.get(2).draws());

        assertEquals("Adam", leaderboard.get(3).profile().displayName());
        assertEquals(1L, leaderboard.get(3).losses());
        assertEquals("Zdena", leaderboard.get(4).profile().displayName());
    }

    private static GameResult networkResult() {
        Instant started = Instant.parse("2026-07-16T12:00:00Z");
        PlayerProfile redProfile = new PlayerProfile(UUID.randomUUID(), "Jana", started, false);
        PlayerProfile blueProfile = new PlayerProfile(UUID.randomUUID(), "Petr", started, false);
        PlayerResult red = PlayerResult.forProfile(PlayerResult.Seat.RED, redProfile,
                15, 60, PlayerResult.Outcome.WIN);
        PlayerResult blue = PlayerResult.forProfile(PlayerResult.Seat.BLUE, blueProfile,
                10, 70, PlayerResult.Outcome.LOSS);
        return new GameResult(UUID.randomUUID(), started, started.plusSeconds(130),
                GameResult.Mode.NETWORK, GameResult.FinishReason.BOARD_FULL,
                5, 5, 120, 130, false, null, red, blue);
    }

    private static GameResult networkResult(PlayerProfile redProfile, int redScore,
                                            PlayerResult.Outcome redOutcome,
                                            PlayerProfile blueProfile, int blueScore,
                                            PlayerResult.Outcome blueOutcome) {
        Instant started = Instant.parse("2026-07-16T12:00:00Z");
        PlayerResult red = PlayerResult.forProfile(PlayerResult.Seat.RED, redProfile,
                redScore, 60, redOutcome);
        PlayerResult blue = PlayerResult.forProfile(PlayerResult.Seat.BLUE, blueProfile,
                blueScore, 70, blueOutcome);
        return new GameResult(UUID.randomUUID(), started, started.plusSeconds(130),
                GameResult.Mode.NETWORK, GameResult.FinishReason.BOARD_FULL,
                5, 5, 120, 130, false, null, red, blue);
    }
}
