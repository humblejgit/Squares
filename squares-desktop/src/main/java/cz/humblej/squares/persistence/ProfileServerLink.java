package cz.humblej.squares.persistence;

import java.time.Instant;
import java.util.UUID;

public final class ProfileServerLink {
    private final UUID localProfileId;
    private final UUID playerId;
    private final UUID installationId;
    private final Instant linkedAt;

    public ProfileServerLink(UUID localProfileId, UUID playerId,
                             UUID installationId, Instant linkedAt) {
        this.localProfileId = localProfileId;
        this.playerId = playerId;
        this.installationId = installationId;
        this.linkedAt = linkedAt;
    }

    public UUID localProfileId() {
        return localProfileId;
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID installationId() {
        return installationId;
    }

    public Instant linkedAt() {
        return linkedAt;
    }
}
