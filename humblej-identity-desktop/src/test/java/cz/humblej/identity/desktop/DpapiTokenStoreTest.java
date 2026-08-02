package cz.humblej.identity.desktop;

import cz.humblej.identity.client.TokenSet;

import com.sun.jna.Platform;
import org.junit.Assume;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DpapiTokenStoreTest {
    @Test
    public void encryptsRestoresAndClearsTokensForCurrentWindowsUser() throws Exception {
        Assume.assumeTrue(Platform.isWindows());
        Path directory = Files.createTempDirectory("identity-dpapi-test-");
        Path path = directory.resolve("session.dat");
        DpapiTokenStore store = new DpapiTokenStore(path);
        TokenSet expected = new TokenSet("secret-access", "secret-refresh", "secret-id",
                Instant.parse("2026-07-27T13:00:00Z"));
        try {
            store.save(expected);

            byte[] diskBytes = Files.readAllBytes(path);
            assertFalse(new String(diskBytes, "UTF-8").contains("secret-access"));
            TokenSet actual = store.load();
            assertEquals(expected.getAccessToken(), actual.getAccessToken());
            assertEquals(expected.getRefreshToken(), actual.getRefreshToken());
            assertEquals(expected.getAccessTokenExpiresAt(), actual.getAccessTokenExpiresAt());

            store.clear();
            assertNull(store.load());
            assertFalse(Files.exists(path));
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }
}
