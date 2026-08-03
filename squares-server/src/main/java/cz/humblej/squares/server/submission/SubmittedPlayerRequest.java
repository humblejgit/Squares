package cz.humblej.squares.server.submission;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import cz.humblej.squares.model.PlayerResult;

public record SubmittedPlayerRequest(
        @NotNull PlayerResult.Seat seat,
        @NotNull PlayerResult.PlayerType playerType,
        UUID playerId,
        @NotBlank @Size(max = 40) String displayNameSnapshot,
        @NotNull @Min(0) Integer score,
        @NotNull @Min(0) Integer thinkingSeconds,
        @NotNull PlayerResult.Outcome outcome) {
}
