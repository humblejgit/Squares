package cz.humblej.squares.server.metadata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("squares.metadata")
public record MetadataProperties(
        String apiVersion,
        int currentRulesVersion,
        String minimumWindowsVersion,
        String minimumAndroidVersion) {
}
