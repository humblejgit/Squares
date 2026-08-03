package cz.humblej.identity.server;

import java.time.Instant;
import java.util.UUID;

record PlayerIdentityRecord(UUID playerId, UUID accountId, Instant createdAt) {
}
