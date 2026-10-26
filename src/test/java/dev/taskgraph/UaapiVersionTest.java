package dev.taskgraph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UaapiVersionTest {

    @ParameterizedTest
    @ValueSource(strings = {"3.0.0-beta-4", "3.0.0", "3.1.2", " 3.0.0-beta-5 ", "3"})
    void acceptsMajorVersionThree(String version) {
        assertTrue(UaapiVersion.isSupported(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.8.2", "2.8.1", "1.15.2-1.0.2", "4.0.0", "", "beta", "v3.0.0"})
    void rejectsOtherVersions(String version) {
        assertFalse(UaapiVersion.isSupported(version));
    }

    @Test
    void rejectsMissingVersion() {
        assertFalse(UaapiVersion.isSupported(null));
    }

    @Test
    void messageNamesInstalledAndRequiredVersions() {
        String message = UaapiVersion.unsupportedMessage("2.8.2");

        assertTrue(message.contains("2.8.2"));
        assertTrue(message.contains(UaapiVersion.REQUIRED));
        assertTrue(message.contains(UaapiVersion.DOWNLOAD_URL));
    }
}
