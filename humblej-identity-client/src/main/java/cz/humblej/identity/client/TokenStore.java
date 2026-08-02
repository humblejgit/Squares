package cz.humblej.identity.client;

import java.io.IOException;

public interface TokenStore {
    TokenSet load() throws IOException;

    void save(TokenSet tokens) throws IOException;

    void clear() throws IOException;
}
