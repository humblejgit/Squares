package cz.codex.squares;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

final class BuildInfo {
    private static final String BUILD_ID = "2.1.0";
    private static final String BUILD_TIME = resolveBuildTime();

    private BuildInfo() {
    }

    static String displayText() {
        return Messages.BUILD_INFO_PREFIX + BUILD_ID + " (" + Messages.BUILD_FILE_TIME_PREFIX + BUILD_TIME + ")";
    }

    static String buildId() {
        return BUILD_ID;
    }

    private static String resolveBuildTime() {
        try {
            URL location = SquaresApp.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(location.toURI());
            long lastModified = file.lastModified();

            if (lastModified > 0L) {
                return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(lastModified));
            }
        } catch (SecurityException | URISyntaxException exception) {
            // Build time is informational only.
        }

        return Messages.BUILD_INFO_UNKNOWN;
    }
}
