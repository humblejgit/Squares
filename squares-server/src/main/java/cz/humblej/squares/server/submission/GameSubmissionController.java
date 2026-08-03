package cz.humblej.squares.server.submission;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/game-submissions/{gameId}")
public class GameSubmissionController {
    private final GameSubmissionService service;

    GameSubmissionController(GameSubmissionService service) {
        this.service = service;
    }

    @GetMapping
    public GameSubmissionStatusResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("gameId") UUID gameId) {
        return service.get(jwt, gameId);
    }

    @PutMapping
    public ResponseEntity<GameSubmissionStatusResponse> put(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("gameId") UUID gameId,
            @RequestHeader("X-Squares-Installation-Id") UUID installationId,
            @Valid @RequestBody PutGameSubmissionRequest request) {
        SubmissionMutation mutation = service.put(jwt, gameId, installationId, request);
        if (mutation.created()) {
            return ResponseEntity.created(URI.create(
                    "/api/v1/me/game-submissions/" + gameId)).body(mutation.status());
        }
        return ResponseEntity.ok(mutation.status());
    }
}
