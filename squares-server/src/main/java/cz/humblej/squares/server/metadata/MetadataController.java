package cz.humblej.squares.server.metadata;

import java.time.Clock;
import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MetadataController {
    private final MetadataProperties properties;
    private final Clock clock;

    public MetadataController(MetadataProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping("/meta")
    public MetadataResponse getMetadata() {
        return new MetadataResponse(
                properties.apiVersion(),
                properties.currentRulesVersion(),
                properties.minimumWindowsVersion(),
                properties.minimumAndroidVersion(),
                Instant.now(clock));
    }
}
