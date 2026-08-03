package cz.humblej.squares.server.submission;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerResult;

public record PutGameSubmissionRequest(
        @NotNull @Min(1) Integer rulesVersion,
        @NotBlank @Pattern(regexp = "^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z.-]+)?$") String coreVersion,
        @NotNull GameResult.Mode mode,
        @NotNull GameResult.FinishReason finishReason,
        @NotNull Instant startedAt,
        @NotNull Instant finishedAt,
        @NotNull @Min(5) @Max(10) Integer rows,
        @NotNull @Min(5) @Max(10) Integer columns,
        @NotNull @Min(0) Integer thinkingTimeLimitSeconds,
        @NotNull @Min(0) Integer totalSeconds,
        @NotNull Boolean randomInitialEdges,
        GameResult.CpuDifficulty cpuDifficulty,
        @NotNull PlayerResult.Seat submittedBySeat,
        @NotNull @Size(min = 2, max = 2) List<@Valid SubmittedPlayerRequest> players) {
}
