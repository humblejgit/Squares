package cz.humblej.identity.server;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
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
    public MeResponse getMe(Jwt jwt) {
        ResolvedIdentity identity = resolveIdentity(jwt);
        return new MeResponse(
                identity.accountStatus(),
                identity.playerId(),
                identity.accountCreatedAt());
    }

    @Transactional
    public ResolvedIdentity resolveIdentity(Jwt jwt) {
        AccountRecord account = accountProvisioning.resolveAccount(jwt);
        PlayerIdentityRecord identity = resolvePlayerIdentity(account);
        return resolved(account, identity);
    }

    @Transactional
    public ResolvedIdentity resolveIdentityForUpdate(Jwt jwt) {
        AccountRecord account = accountProvisioning.resolveAccount(jwt);
        PlayerIdentityRecord identity = resolvePlayerIdentity(account);
        repository.lockAccount(account.accountId());
        return resolved(account, identity);
    }

    @Transactional
    InstallationMutation putInstallation(
            Jwt jwt, UUID installationId, PutInstallationRequest request) {
        ResolvedIdentity identity = resolveIdentityForUpdate(jwt);
        InstallationRecord existing = repository
                .findInstallation(identity.accountId(), installationId)
                .orElse(null);
        Instant now = Instant.now(clock);
        InstallationRecord installation = existing == null
                ? repository.insertInstallation(identity.accountId(), installationId, request, now)
                : repository.updateInstallation(identity.accountId(), installationId, request, now);
        return new InstallationMutation(InstallationResponse.from(installation), existing == null);
    }

    private PlayerIdentityRecord resolvePlayerIdentity(AccountRecord account) {
        PlayerIdentityRecord identity = repository.findPlayerIdentity(account.accountId()).orElse(null);
        if (identity != null) {
            return identity;
        }

        repository.lockAccount(account.accountId());
        identity = repository.findPlayerIdentity(account.accountId()).orElse(null);
        return identity != null
                ? identity
                : repository.insertPlayerIdentity(
                        UUID.randomUUID(), account.accountId(), Instant.now(clock));
    }

    private static ResolvedIdentity resolved(
            AccountRecord account, PlayerIdentityRecord identity) {
        return new ResolvedIdentity(
                account.accountId(), account.status(), identity.playerId(), account.createdAt());
    }

}
