package cz.humblej.identity.desktop;

import cz.humblej.identity.client.TokenSet;
import cz.humblej.identity.client.TokenStore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Crypt32Util;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class DpapiTokenStore implements TokenStore {
    private final Path path;
    private final ObjectMapper mapper;

    public DpapiTokenStore(Path path) {
        this.path = path.toAbsolutePath().normalize();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public TokenSet load() throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        ensureWindows();
        try {
            byte[] clear = Crypt32Util.cryptUnprotectData(Files.readAllBytes(path));
            return mapper.readValue(clear, TokenSet.class);
        } catch (RuntimeException exception) {
            throw new IOException("Uloženou přihlašovací relaci se nepodařilo odemknout.", exception);
        }
    }

    @Override
    public void save(TokenSet tokens) throws IOException {
        ensureWindows();
        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException("Cesta pro přihlašovací relaci nemá nadřazenou složku.");
        }
        Files.createDirectories(parent);

        byte[] encrypted;
        try {
            encrypted = Crypt32Util.cryptProtectData(mapper.writeValueAsBytes(tokens));
        } catch (RuntimeException exception) {
            throw new IOException("Přihlašovací relaci se nepodařilo zabezpečit pomocí Windows DPAPI.", exception);
        }

        Path temporary = Files.createTempFile(parent, "session-", ".tmp");
        try {
            Files.write(temporary, encrypted);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void clear() throws IOException {
        try {
            Files.deleteIfExists(path);
        } catch (IOException deleteFailure) {
            try {
                Files.write(path, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException overwriteFailure) {
                deleteFailure.addSuppressed(overwriteFailure);
                throw deleteFailure;
            }
        }
    }

    private static void ensureWindows() throws IOException {
        if (!Platform.isWindows()) {
            throw new IOException("Bezpečné trvalé uložení přihlášení je podporováno pouze ve Windows.");
        }
    }
}
