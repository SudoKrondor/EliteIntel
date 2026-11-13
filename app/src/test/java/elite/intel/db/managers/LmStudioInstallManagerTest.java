package elite.intel.db.managers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * One install record at most, recorded unfinished, then marked finished or forgotten.
 */
class LmStudioInstallManagerTest {

    private final LmStudioInstallManager installs = LmStudioInstallManager.getInstance();

    // WHY: the suite shares one in-memory database, so a record left by any test would change what the next one sees.
    @BeforeEach
    @AfterEach
    void noRecord() {
        installs.forget();
    }

    @Test
    void nothingIsRecordedUntilAnInstallStarts() {
        assertTrue(installs.find().isEmpty());
    }

    @Test
    void anInstallIsRecordedUnfinishedWithEverythingItWasGiven() {
        Path home = Path.of("D:", "Games", "LMStudio");

        LmStudioInstallManager.Install started = installs.recordStarted(home, true);

        LmStudioInstallManager.Install found = installs.find().orElseThrow();
        assertEquals(started, found);
        assertEquals(home, found.homeDir());
        assertTrue(found.pointerWritten());
        assertFalse(found.setupComplete());
    }

    @Test
    void markingItCompleteKeepsEverythingElse() {
        LmStudioInstallManager.Install started = installs.recordStarted(Path.of("/home/cmdr/.lmstudio"), false);

        installs.markSetupComplete();

        LmStudioInstallManager.Install found = installs.find().orElseThrow();
        assertTrue(found.setupComplete());
        assertEquals(started.installId(), found.installId());
        assertEquals(started.homeDir(), found.homeDir());
        assertEquals(started.installedAt(), found.installedAt());
        assertFalse(found.pointerWritten());
    }

    @Test
    void thereIsOneRecordAtMost() {
        installs.recordStarted(Path.of("/home/cmdr/.lmstudio"), false);

        assertThrows(IllegalStateException.class,
                () -> installs.recordStarted(Path.of("/mnt/games/LMStudio"), true));
    }

    @Test
    void nothingToMarkCompleteIsAnError() {
        assertThrows(IllegalStateException.class, installs::markSetupComplete);
    }

    @Test
    void forgettingRemovesTheRecord() {
        installs.recordStarted(Path.of("/home/cmdr/.lmstudio"), false);

        installs.forget();

        assertTrue(installs.find().isEmpty());
    }

    @Test
    void aReclaimedInstallIsRecordedAsGivenInPlaceOfAnyRecord() {
        installs.recordStarted(Path.of("/home/cmdr/.lmstudio"), false);
        UUID installId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-12T21:14:40Z");

        LmStudioInstallManager.Install reclaimed =
                installs.recordReclaimed(installId, Path.of("/mnt/games/LMStudio"), true, true, now);

        assertEquals(new LmStudioInstallManager.Install(installId, Path.of("/mnt/games/LMStudio"), true, true, now),
                reclaimed);
        assertEquals(reclaimed, installs.find().orElseThrow());
    }
}
