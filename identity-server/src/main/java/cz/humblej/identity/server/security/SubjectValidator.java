package cz.humblej.identity.server.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

final class SubjectValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error MISSING_SUBJECT = new OAuth2Error(
            "invalid_token",
            "The token does not contain a subject.",
            null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (StringUtils.hasText(token.getSubject())) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(MISSING_SUBJECT);
    }
}
