package cz.humblej.identity.desktop;

import cz.humblej.identity.client.TokenSet;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OidcClientTest {
    @Test
    public void createsRfc7636S256Challenge() throws Exception {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
                OidcClient.sha256Base64Url(
                        "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }

    @Test
    public void tokenExpiryUsesSafetyWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC);
        TokenSet token = new TokenSet("access", "refresh", "id",
                Instant.parse("2026-07-27T12:01:00Z"));

        assertTrue(token.expiresWithin(clock, 60));
        assertFalse(token.expiresWithin(clock, 59));
        assertTrue(token.canRefresh());
    }
}
