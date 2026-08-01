package cz.humblej.squares.server.identity;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class IdentityController {
    private final IdentityService service;

    IdentityController(IdentityService service) {
        this.service = service;
    }

    @GetMapping
    public MeResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return service.getMe(jwt);
    }

    @PutMapping("/profile")
    public ResponseEntity<PublicPlayerResponse> putProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PutProfileRequest request) {
        ProfileMutation mutation = service.putProfile(jwt, request);
        String etag = "\"" + mutation.player().revision() + "\"";

        if (mutation.created()) {
            URI location = URI.create("/api/v1/players/" + mutation.player().playerId());
            return ResponseEntity.created(location)
                    .eTag(etag)
                    .body(mutation.player());
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .body(mutation.player());
    }
}
