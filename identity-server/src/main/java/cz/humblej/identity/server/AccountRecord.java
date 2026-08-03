package cz.humblej.identity.server;

import java.time.Instant;
import java.util.UUID;

record AccountRecord(UUID accountId, String status, Instant createdAt) {
}
