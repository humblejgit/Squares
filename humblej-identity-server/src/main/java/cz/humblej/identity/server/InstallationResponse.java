package cz.humblej.identity.server;

import java.time.Instant;
import java.util.UUID;

public record InstallationResponse(
        UUID installationId,
        String platform,
        String appVersion,
        String coreVersion,
        String locale,
        Instant createdAt,
        Instant lastSeenAt) {
    static InstallationResponse from(InstallationRecord installation) {
        return new InstallationResponse(
                installation.installationId(),
                installation.platform(),
                installation.appVersion(),
                installation.coreVersion(),
                installation.locale(),
                installation.createdAt(),
                installation.lastSeenAt());
    }
}
