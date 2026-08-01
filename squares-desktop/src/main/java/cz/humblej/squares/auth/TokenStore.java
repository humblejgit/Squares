package cz.humblej.squares.auth;

import java.io.IOException;

interface TokenStore {
    TokenSet load() throws IOException;

    void save(TokenSet tokens) throws IOException;

    void clear() throws IOException;
}
