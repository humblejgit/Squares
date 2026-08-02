package cz.humblej.identity.server;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PutInstallationRequest(
        @NotNull
        @Pattern(regexp = "WINDOWS|ANDROID")
        String platform,
        @NotNull
        @Pattern(regexp = "^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z.-]+)?$")
        String appVersion,
        @NotNull
        @Pattern(regexp = "^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z.-]+)?$")
        String coreVersion,
        @NotNull
        @Size(min = 2, max = 35)
        String locale) {
}
