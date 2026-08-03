package cz.humblej.identity.client;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** Message keys and lookup for the reusable identity client. */
public final class IdentityClientMessages {
    public static final String HTTPS_REQUIRED = "identity.client.https-required";
    public static final String SERVER_UNAVAILABLE = "identity.client.server-unavailable";
    public static final String SESSION_MISSING = "identity.client.session-missing";
    public static final String SESSION_STORE_FAILED = "identity.client.session-store-failed";
    public static final String HTTP_ERROR = "identity.client.http-error";

    private static final String BUNDLE = "i18n.identity-client";

    private IdentityClientMessages() {
    }

    public static String get(String key, Object... arguments) {
        Locale locale = Locale.getDefault();
        String pattern = ResourceBundle.getBundle(BUNDLE, locale).getString(key);
        return new MessageFormat(pattern, locale).format(arguments);
    }
}
