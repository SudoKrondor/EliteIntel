package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallMarkerTest {

    @Test
    void aMarkerMatchesTheInstallThatWroteIt(@TempDir Path home) throws IOException {
        UUID installId = UUID.randomUUID();

        InstallMarker.write(home, installId);

        assertTrue(InstallMarker.matches(home, installId));
    }

    @Test
    void aMarkerFromAnotherInstallDoesNotMatch(@TempDir Path home) throws IOException {
        InstallMarker.write(home, UUID.randomUUID());

        assertFalse(InstallMarker.matches(home, UUID.randomUUID()));
    }

    @Test
    void aFolderWithoutAMarkerIsNotTheAppsInstall(@TempDir Path home) {
        assertFalse(InstallMarker.matches(home, UUID.randomUUID()), "an LM Studio someone else installed");
        assertFalse(InstallMarker.matches(home.resolve("gone"), UUID.randomUUID()), "a folder that does not exist");
    }

    @Test
    void surroundingWhitespaceIsNotADifferentId(@TempDir Path home) throws IOException {
        UUID installId = UUID.randomUUID();
        Files.writeString(home.resolve(InstallMarker.FILE), installId + "\r\n");

        assertTrue(InstallMarker.matches(home, installId));
    }

    @Test
    void readGivesTheIdAMarkerHolds(@TempDir Path home) throws IOException {
        UUID installId = UUID.randomUUID();
        InstallMarker.write(home, installId);

        assertEquals(Optional.of(installId), InstallMarker.read(home));
    }

    @Test
    void aMarkerThatHoldsNoIdReadsAsNone(@TempDir Path home) throws IOException {
        Files.writeString(home.resolve(InstallMarker.FILE), "not an id");

        assertTrue(InstallMarker.read(home).isEmpty());
        assertTrue(InstallMarker.read(home.resolve("gone")).isEmpty(), "a folder that does not exist");
    }
}
