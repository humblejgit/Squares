package cz.humblej.identity.server;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountProvisioningService {
    private final IdentityRepository repository;
    private final Clock clock;

    AccountProvisioningService(IdentityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    AccountRecord resolveAccount(Jwt jwt) {
        String issuer = jwt.getIssuer().toString();
        String subject = jwt.getSubject();
        AccountRecord existing = repository.findAccount(issuer, subject).orElse(null);

        if (existing != null) {
            return existing;
        }

        UUID candidateId = UUID.randomUUID();
        Instant now = Instant.now(clock);
        repository.insertAccount(candidateId, now);

        if (repository.insertIdentity(issuer, subject, candidateId, now) == 1) {
            return new AccountRecord(candidateId, "ACTIVE", now);
        }

        repository.deleteUnlinkedAccount(candidateId);
        return repository.findAccount(issuer, subject)
                .orElseThrow(() -> new IllegalStateException(
                        "Concurrent identity creation did not produce an account."));
    }
}
