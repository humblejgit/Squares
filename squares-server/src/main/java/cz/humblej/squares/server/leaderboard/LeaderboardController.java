package cz.humblej.squares.server.leaderboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class LeaderboardController {
    private final LeaderboardService service;

    LeaderboardController(LeaderboardService service) {
        this.service = service;
    }

    @GetMapping("/leaderboards/casual")
    public LeaderboardPageResponse getCasualLeaderboard(
            @RequestParam(name = "limit", defaultValue = "50")
            @Min(1) @Max(100) int limit,
            @RequestParam(name = "cursor", required = false) String cursor) {
        return service.getCasualPage(limit, cursor);
    }

    @GetMapping("/me/leaderboards/casual")
    public ResponseEntity<LeaderboardEntryResponse> getMyCasualLeaderboardEntry(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.of(service.getMyCasualEntry(jwt));
    }
}
