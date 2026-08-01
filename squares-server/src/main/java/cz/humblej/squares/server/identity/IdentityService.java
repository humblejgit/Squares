package cz.humblej.squares.server.identity;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IdentityService {
    private final IdentityRepository repository;
    private final AccountProvisioningService accountProvisioning;
    private final Clock clock;

    IdentityService(
            IdentityRepository repository,
            AccountProvisioningService accountProvisioning,
            Clock clock) {
        this.repository = repository;
        this.accountProvisioning = accountProvisioning;
        this.clock = clock;
    }

    @Transactional
    MeResponse getMe(Jwt jwt) {
        AccountRecord account = accountProvisioning.resolveAccount(jwt);
        PublicPlayerResponse player = repository.findPlayer(account.accountId())
                .map(PublicPlayerResponse::from)
                .orElse(null);
        return new MeResponse(
                account.status(),
                player == null,
                player,
                account.createdAt());
    }

    @Transactional
    ProfileMutation putProfile(Jwt jwt, PutProfileRequest request) {
        AccountRecord account = accountProvisioning.resolveAccount(jwt);
        repository.lockAccount(account.accountId());

        String handle = request.handle().toLowerCase(Locale.ROOT);
        PlayerRecord existing = repository.findPlayer(account.accountId()).orElse(null);

        if (existing != null
                && Objects.equals(existing.handle(), handle)
                && Objects.equals(existing.displayName(), request.displayName())) {
            return new ProfileMutation(PublicPlayerResponse.from(existing), false);
        }

        Instant now = Instant.now(clock);

        try {
            if (existing == null) {
                PlayerRecord created = repository.insertPlayer(
                        UUID.randomUUID(),
                        account.accountId(),
                        handle,
                        handle,
                        request.displayName(),
                        now);
                return new ProfileMutation(PublicPlayerResponse.from(created), true);
            }

            PlayerRecord updated = repository.updatePlayer(
                    account.accountId(),
                    handle,
                    handle,
                    request.displayName(),
                    now);
            return new ProfileMutation(PublicPlayerResponse.from(updated), false);
        } catch (DuplicateKeyException exception) {
            throw new HandleUnavailableException();
        }
    }

}
