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
@RequestMapping("/api/v1/me/profile")
public class ProfileController {
    private final ProfileService service;

    ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PublicPlayerResponse> getProfile(
            @AuthenticationPrincipal Jwt jwt) {
        PublicPlayerResponse profile = service.getProfile(jwt);
        return profile == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok().eTag("\"" + profile.revision() + "\"").body(profile);
    }

    @PutMapping
    public ResponseEntity<PublicPlayerResponse> putProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PutProfileRequest request) {
        ProfileMutation mutation = service.putProfile(jwt, request);
        String etag = "\"" + mutation.player().revision() + "\"";
        if (mutation.created()) {
            URI location = URI.create("/api/v1/players/" + mutation.player().playerId());
            return ResponseEntity.created(location).eTag(etag).body(mutation.player());
        }
        return ResponseEntity.ok().eTag(etag).body(mutation.player());
    }
}
