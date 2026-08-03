package cz.humblej.identity.client;

import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdentityClientMessagesTest {
    @Test
    public void resolvesCzechAndEnglishMessages() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("cs-CZ"));
            assertEquals("Server identity není dostupný.",
                    IdentityClientMessages.get(IdentityClientMessages.SERVER_UNAVAILABLE));

            Locale.setDefault(Locale.ENGLISH);
            assertEquals("Identity server is unavailable.",
                    IdentityClientMessages.get(IdentityClientMessages.SERVER_UNAVAILABLE));
        } finally {
            Locale.setDefault(original);
        }
    }
}
