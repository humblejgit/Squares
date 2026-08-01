package cz.humblej.squares.server.metadata;

import java.time.Instant;

public record MetadataResponse(
        String apiVersion,
        int currentRulesVersion,
        String minimumWindowsVersion,
        String minimumAndroidVersion,
        Instant serverTime) {
}
