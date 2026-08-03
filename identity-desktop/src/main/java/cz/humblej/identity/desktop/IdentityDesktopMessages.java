package cz.humblej.identity.desktop;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** Message keys and lookup for the desktop OIDC adapter. */
public final class IdentityDesktopMessages {
    public static final String LOGIN_TIMEOUT = "identity.desktop.login-timeout";
    public static final String CALLBACK_EMPTY = "identity.desktop.callback-empty";
    public static final String CALLBACK_INVALID_STATE = "identity.desktop.callback-invalid-state";
    public static final String PROVIDER_ERROR = "identity.desktop.provider-error";
    public static final String AUTHORIZATION_CODE_MISSING = "identity.desktop.authorization-code-missing";
    public static final String LOGIN_INTERRUPTED = "identity.desktop.login-interrupted";
    public static final String LOGIN_FAILED = "identity.desktop.login-failed";
    public static final String REFRESH_EXPIRED = "identity.desktop.refresh-expired";
    public static final String REFRESH_FAILED = "identity.desktop.refresh-failed";
    public static final String DISCOVERY_HTTP_ERROR = "identity.desktop.discovery-http-error";
    public static final String DISCOVERY_ISSUER_MISMATCH = "identity.desktop.discovery-issuer-mismatch";
    public static final String DISCOVERY_FAILED = "identity.desktop.discovery-failed";
    public static final String TOKEN_REJECTED = "identity.desktop.token-rejected";
    public static final String TOKEN_REJECTED_WITH_DETAIL = "identity.desktop.token-rejected-with-detail";
    public static final String TOKEN_EXPIRATION_MISSING = "identity.desktop.token-expiration-missing";
    public static final String TOKEN_RESPONSE_INVALID = "identity.desktop.token-response-invalid";
    public static final String SHA256_UNAVAILABLE = "identity.desktop.sha256-unavailable";
    public static final String BROWSER_UNAVAILABLE = "identity.desktop.browser-unavailable";
    public static final String BROWSER_OPEN_FAILED = "identity.desktop.browser-open-failed";
    public static final String BROWSER_SUCCESS_TITLE = "identity.desktop.browser-success-title";
    public static final String BROWSER_FAILURE_TITLE = "identity.desktop.browser-failure-title";
    public static final String BROWSER_CLOSE = "identity.desktop.browser-close";
    public static final String ENDPOINT_HTTPS_REQUIRED = "identity.desktop.endpoint-https-required";
    public static final String RESPONSE_FIELD_MISSING = "identity.desktop.response-field-missing";
    public static final String STORE_UNLOCK_FAILED = "identity.desktop.store-unlock-failed";
    public static final String STORE_PARENT_MISSING = "identity.desktop.store-parent-missing";
    public static final String STORE_PROTECT_FAILED = "identity.desktop.store-protect-failed";
    public static final String STORE_WINDOWS_ONLY = "identity.desktop.store-windows-only";

    private static final String BUNDLE = "i18n.identity-desktop";

    private IdentityDesktopMessages() {
    }

    public static String get(String key, Object... arguments) {
        Locale locale = Locale.getDefault();
        String pattern = ResourceBundle.getBundle(BUNDLE, locale).getString(key);
        return new MessageFormat(pattern, locale).format(arguments);
    }
}
