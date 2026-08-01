package cz.humblej.squares.server.identity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PutProfileRequest(
        @NotNull
        @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{2,23}$")
        String handle,
        @NotNull
        @Size(min = 1, max = 40)
        String displayName) {
}
