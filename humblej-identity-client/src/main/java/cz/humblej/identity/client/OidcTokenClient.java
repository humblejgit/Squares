package cz.humblej.identity.client;

/** Platform adapter for an OIDC authorization flow and token endpoint. */
public interface OidcTokenClient {
    TokenSet login() throws AuthenticationException;

    TokenSet refresh(TokenSet current) throws AuthenticationException;

    void revoke(TokenSet tokens);
}
