package dev.taskgraph;

/**
 * Which UltimateAdvancementAPI versions the plugin works with. It is compiled against the 3.x API, whose classes
 * differ from 2.x (e.g. {@code AbstractAdvancementDisplay} doesn't exist there), so any other major version fails
 * at runtime with a {@link NoClassDefFoundError}. Checking up front gives the server owner a clear message instead.
 */
final class UaapiVersion {

    static final String PLUGIN_NAME = "UltimateAdvancementAPI";
    static final String REQUIRED = "3.0.0-beta-4";
    static final String DOWNLOAD_URL = "https://modrinth.com/plugin/ultimateadvancementapi/versions";

    private static final int SUPPORTED_MAJOR = 3;

    private UaapiVersion() {
    }

    /** Whether {@code version} (e.g. "3.0.0-beta-4") has the supported major version. */
    static boolean isSupported(String version) {
        if (version == null) {
            return false;
        }
        String major = version.strip().split("[.\\-+]", 2)[0];
        try {
            return Integer.parseInt(major) == SUPPORTED_MAJOR;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    static String unsupportedMessage(String installed) {
        return "TaskGraph needs " + PLUGIN_NAME + " " + REQUIRED + ", but " + installed + " is installed. "
                + "Download " + REQUIRED + " (marked as beta, not the Mojang-Mapped-Legacy jar) from " + DOWNLOAD_URL
                + ", replace the current jar in plugins/ and restart. Disabling TaskGraph.";
    }
}
