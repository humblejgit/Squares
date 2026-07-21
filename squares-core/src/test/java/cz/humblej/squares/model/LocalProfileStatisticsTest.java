package cz.humblej.squares.model;

import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class LocalProfileStatisticsTest {
    @Test
    public void winPercentageUsesAllGamesAndHandlesEmptyHistory() {
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Jana", Instant.EPOCH, false);

        LocalProfileStatistics played = new LocalProfileStatistics(profile, 4, 2, 1, 1, 30);
        LocalProfileStatistics empty = new LocalProfileStatistics(profile, 0, 0, 0, 0, 0);

        assertEquals(50.0, played.winPercentage(), 0.001);
        assertEquals(0.0, empty.winPercentage(), 0.001);
    }
}
