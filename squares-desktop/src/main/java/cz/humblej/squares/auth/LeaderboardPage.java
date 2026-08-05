package cz.humblej.squares.auth;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LeaderboardPage {
    private final List<LeaderboardEntry> entries;
    private final String nextCursor;
    private final Instant generatedAt;

    public LeaderboardPage(
            List<LeaderboardEntry> entries, String nextCursor, Instant generatedAt) {
        this.entries = Collections.unmodifiableList(
                new ArrayList<LeaderboardEntry>(entries));
        this.nextCursor = nextCursor;
        this.generatedAt = generatedAt;
    }

    public List<LeaderboardEntry> entries() { return entries; }
    public String nextCursor() { return nextCursor; }
    public Instant generatedAt() { return generatedAt; }
}
