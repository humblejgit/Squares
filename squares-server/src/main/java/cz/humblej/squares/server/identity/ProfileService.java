package cz.humblej.squares.server.identity;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.humblej.identity.server.IdentityService;
import cz.humblej.identity.server.ResolvedIdentity;

@Service
class ProfileService {
    private final ProfileRepository repository;
    private final IdentityService identityService;
    private final Clock clock;

    ProfileService(ProfileRepository repository, IdentityService identityService, Clock clock) {
        this.repository = repository;
        this.identityService = identityService;
        this.clock = clock;
    }

    @Transactional
    PublicPlayerResponse getProfile(Jwt jwt) {
        ResolvedIdentity identity = identityService.resolveIdentity(jwt);
        return repository.findPlayer(identity.playerId())
                .map(PublicPlayerResponse::from)
                .orElse(null);
    }

    @Transactional
    ProfileMutation putProfile(Jwt jwt, PutProfileRequest request) {
        ResolvedIdentity identity = identityService.resolveIdentityForUpdate(jwt);
        String handle = request.handle().toLowerCase(Locale.ROOT);
        PlayerRecord existing = repository.findPlayer(identity.playerId()).orElse(null);

        if (existing != null
                && Objects.equals(existing.handle(), handle)
                && Objects.equals(existing.displayName(), request.displayName())) {
            return new ProfileMutation(PublicPlayerResponse.from(existing), false);
        }

        Instant now = Instant.now(clock);
        try {
            if (existing == null) {
                PlayerRecord created = repository.insertPlayer(
                        identity.playerId(), handle, handle, request.displayName(), now);
                return new ProfileMutation(PublicPlayerResponse.from(created), true);
            }
            PlayerRecord updated = repository.updatePlayer(
                    identity.playerId(), handle, handle, request.displayName(), now);
            return new ProfileMutation(PublicPlayerResponse.from(updated), false);
        } catch (DuplicateKeyException exception) {
            throw new HandleUnavailableException();
        }
    }
}
