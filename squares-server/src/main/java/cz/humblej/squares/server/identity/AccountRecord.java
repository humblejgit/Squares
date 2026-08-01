package cz.humblej.squares.server.identity;

import java.time.Instant;
import java.util.UUID;

record AccountRecord(UUID accountId, String status, Instant createdAt) {
}
