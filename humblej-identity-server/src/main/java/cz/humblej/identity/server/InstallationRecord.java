package cz.humblej.identity.server;

import java.time.Instant;
import java.util.UUID;

record InstallationRecord(
        UUID installationId,
        String platform,
        String appVersion,
        String coreVersion,
        String locale,
        Instant createdAt,
        Instant lastSeenAt) {
}
