package cz.humblej.identity.server;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PutMapping("/installations/{installationId}")
    public ResponseEntity<InstallationResponse> putInstallation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("installationId") UUID installationId,
            @Valid @RequestBody PutInstallationRequest request) {
        InstallationMutation mutation = service.putInstallation(jwt, installationId, request);
        if (mutation.created()) {
            URI location = URI.create("/api/v1/me/installations/" + installationId);
            return ResponseEntity.created(location).body(mutation.installation());
        }
        return ResponseEntity.ok(mutation.installation());
    }
}
