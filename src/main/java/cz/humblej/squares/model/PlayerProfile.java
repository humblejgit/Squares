package cz.humblej.squares.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Stable identity of a human player. The display name may change while the id
 * remains the same; completed games keep their own display-name snapshot.
 */
public final class PlayerProfile {
    private final UUID id;
    private final String displayName;
    private final Instant createdAt;
    private final boolean archived;

    public static PlayerProfile create(String displayName) {
        return new PlayerProfile(UUID.randomUUID(), displayName, Instant.now(), false);
    }

    public PlayerProfile(UUID id, String displayName, Instant createdAt, boolean archived) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = requireDisplayName(displayName);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.archived = archived;
    }

    public UUID id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean archived() {
        return archived;
    }

    private static String requireDisplayName(String displayName) {
        String normalized = Objects.requireNonNull(displayName, "displayName").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Display name must not be empty.");
        }

        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof PlayerProfile)) {
            return false;
        }

        PlayerProfile profile = (PlayerProfile) other;
        return id.equals(profile.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
