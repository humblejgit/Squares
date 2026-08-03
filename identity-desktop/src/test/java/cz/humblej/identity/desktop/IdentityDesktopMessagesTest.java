package cz.humblej.identity.desktop;

import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdentityDesktopMessagesTest {
    @Test
    public void resolvesCzechAndEnglishMessages() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("cs-CZ"));
            assertEquals("Přihlášení bylo přerušeno.",
                    IdentityDesktopMessages.get(IdentityDesktopMessages.LOGIN_INTERRUPTED));

            Locale.setDefault(Locale.ENGLISH);
            assertEquals("Login was interrupted.",
                    IdentityDesktopMessages.get(IdentityDesktopMessages.LOGIN_INTERRUPTED));
        } finally {
            Locale.setDefault(original);
        }
    }
}
