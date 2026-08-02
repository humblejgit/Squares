package cz.humblej.identity.server;

import java.time.Instant;
import java.util.UUID;

/** Identity resolved from an authenticated OIDC principal for use by host applications. */
public record ResolvedIdentity(
        UUID accountId,
        String accountStatus,
        UUID playerId,
        Instant accountCreatedAt) {
}
