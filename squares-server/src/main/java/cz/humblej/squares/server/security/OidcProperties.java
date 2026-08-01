package cz.humblej.squares.server.security;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("squares.security.oidc")
public record OidcProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        String jwkSetUri) {
}
