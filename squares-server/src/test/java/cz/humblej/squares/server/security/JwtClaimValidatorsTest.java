package cz.humblej.squares.server.security;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtClaimValidatorsTest {
    @Test
    void acceptsExpectedAudience() {
        Jwt token = token(List.of("other-api", "squares-api"), "subject");

        assertFalse(new AudienceValidator("squares-api").validate(token).hasErrors());
    }

    @Test
    void rejectsMissingAudience() {
        Jwt token = token(List.of("other-api"), "subject");

        assertTrue(new AudienceValidator("squares-api").validate(token).hasErrors());
    }

    @Test
    void rejectsMissingSubject() {
        Jwt token = token(List.of("squares-api"), null);

        assertTrue(new SubjectValidator().validate(token).hasErrors());
    }

    private static Jwt token(List<String> audience, String subject) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("https://identity.squares.test")
                .audience(audience)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));

        if (subject != null) {
            builder.subject(subject);
        }

        return builder.build();
    }
}
