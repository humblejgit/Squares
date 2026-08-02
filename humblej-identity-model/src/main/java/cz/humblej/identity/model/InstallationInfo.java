package cz.humblej.identity.model;

import java.util.Locale;
import java.util.UUID;

public final class InstallationInfo {
    private final UUID installationId;
    private final String platform;
    private final String appVersion;
    private final String coreVersion;
    private final String locale;

    public static InstallationInfo windows(UUID installationId, String version) {
        String languageTag = Locale.getDefault().toLanguageTag();
        if (languageTag == null || languageTag.length() < 2) {
            languageTag = "en";
        }
        return new InstallationInfo(installationId, "WINDOWS", version, version, languageTag);
    }

    public InstallationInfo(UUID installationId, String platform, String appVersion,
                            String coreVersion, String locale) {
        this.installationId = installationId;
        this.platform = platform;
        this.appVersion = appVersion;
        this.coreVersion = coreVersion;
        this.locale = locale;
    }

    public UUID installationId() {
        return installationId;
    }

    public String platform() {
        return platform;
    }

    public String appVersion() {
        return appVersion;
    }

    public String coreVersion() {
        return coreVersion;
    }

    public String locale() {
        return locale;
    }
}
