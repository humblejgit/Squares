package cz.codex.squares;

import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GameResultCodecTest {
    @Test
    public void roundTripPreservesNetworkResultAndProfileIds() throws Exception {
        Instant started = Instant.parse("2026-07-16T10:00:00Z");
        UUID gameId = UUID.randomUUID();
        PlayerProfile redProfile = new PlayerProfile(UUID.randomUUID(), "Anna", started.minusSeconds(60), false);
        PlayerProfile blueProfile = new PlayerProfile(UUID.randomUUID(), "Karel", started.minusSeconds(30), false);
        PlayerResult red = PlayerResult.forProfile(PlayerResult.Seat.RED, redProfile,
                12, 40, PlayerResult.Outcome.LOSS);
        PlayerResult blue = PlayerResult.forProfile(PlayerResult.Seat.BLUE, blueProfile,
                13, 45, PlayerResult.Outcome.WIN);
        GameResult original = new GameResult(gameId, started, started.plusSeconds(85),
                GameResult.Mode.NETWORK, GameResult.FinishReason.BOARD_FULL,
                5, 5, 60, 85, true, null, red, blue);

        GameResult decoded = GameResultCodec.decode(GameResultCodec.encode(original));

        assertEquals(gameId, decoded.gameId());
        assertEquals(original.startedAt(), decoded.startedAt());
        assertEquals(original.finishedAt(), decoded.finishedAt());
        assertEquals(original.mode(), decoded.mode());
        assertEquals(original.finishReason(), decoded.finishReason());
        assertEquals(original.redPlayer().profile().id(), decoded.redPlayer().profile().id());
        assertEquals(original.bluePlayer().profile().id(), decoded.bluePlayer().profile().id());
        assertEquals("Anna", decoded.redPlayer().displayName());
        assertEquals(13, decoded.bluePlayer().score());
        assertFalse(decoded.isDraw());
    }
}
