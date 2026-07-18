package cz.codex.squares;

import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class PlayerProfileTest {
    @Test
    public void profileKeepsStableIdentityAndNormalizedName() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-16T12:00:00Z");
        PlayerProfile profile = new PlayerProfile(id, "  Jana  ", createdAt, false);

        assertEquals(id, profile.id());
        assertEquals("Jana", profile.displayName());
        assertEquals(createdAt, profile.createdAt());
        assertFalse(profile.archived());
    }

    @Test
    public void profilesWithDifferentIdsAreDifferentEvenWithSameName() {
        Instant createdAt = Instant.parse("2026-07-16T12:00:00Z");
        PlayerProfile first = new PlayerProfile(UUID.randomUUID(), "Jana", createdAt, false);
        PlayerProfile second = new PlayerProfile(UUID.randomUUID(), "Jana", createdAt, false);

        assertNotEquals(first, second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankDisplayNameIsRejected() {
        PlayerProfile.create("   ");
    }
}
