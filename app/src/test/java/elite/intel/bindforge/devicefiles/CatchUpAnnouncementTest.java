package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * At startup, an installation that missed a write and is back is said once, spoken and logged - never asked there
 * (Alan, 2026-10-15, A14).
 */
class CatchUpAnnouncementTest {

    @TempDir
    Path temp;

    private final List<String> spoken = new ArrayList<>();
    private final List<String> logged = new ArrayList<>();
    private final List<String> detections = new ArrayList<>();

    @Test
    void anInstallationFoundAgainIsSpokenAndLoggedOnce() throws IOException {
        AtomicBoolean announced = new AtomicBoolean();
        List<Target> targets = List.of(back("EPIC"), new Target(2, "STEAM", dir("steam"), false));

        announcement(true, targets, announced).announceOnce();
        announcement(true, targets, announced).announceOnce();

        assertEquals(1, spoken.size(), "once per run, across a restart of the services");
        assertTrue(spoken.getFirst().contains("EPIC"), spoken.getFirst());
        assertEquals(1, logged.size());
        assertTrue(logged.getFirst().contains("EPIC"), logged.getFirst());
    }

    @Test
    void severalAreNamedInOneLine() throws IOException {
        announcement(true, List.of(back("EPIC"), back("FRONTIER")), new AtomicBoolean()).announceOnce();

        assertEquals(1, spoken.size());
        assertTrue(spoken.getFirst().contains("EPIC") && spoken.getFirst().contains("FRONTIER"), spoken.getFirst());
        assertEquals(2, logged.size(), "a log line each, with its folder");
    }

    @Test
    void nothingFoundAgainSaysNothingAndKeepsTheAnnouncement() throws IOException {
        AtomicBoolean announced = new AtomicBoolean();

        announcement(true, List.of(new Target(1, "STEAM", dir("steam"), false)), announced).announceOnce();

        assertTrue(spoken.isEmpty());
        assertFalse(announced.get(), "a later start may still find one");
    }

    /** Before activation nothing was ever written, so nothing was missed - and detection is not run for it. */
    @Test
    void beforeActivationNothingIsLookedAt() throws IOException {
        announcement(false, List.of(back("EPIC")), new AtomicBoolean()).announceOnce();

        assertTrue(spoken.isEmpty());
        assertTrue(detections.isEmpty());
    }

    @Test
    void detectionRunsBeforeTheInstallationsAreRead() throws IOException {
        announcement(true, List.of(back("EPIC")), new AtomicBoolean()).announceOnce();

        assertEquals(List.of("detected"), detections);
    }

    private CatchUpAnnouncement announcement(boolean activated, List<Target> targets, AtomicBoolean announced) {
        return new CatchUpAnnouncement(() -> activated, () -> detections.add("detected"), () -> targets,
                spoken::add, logged::add, announced);
    }

    private Target back(String storefront) throws IOException {
        return new Target(storefront.hashCode(), storefront, dir(storefront), false, true);
    }

    private Path dir(String name) throws IOException {
        return Files.createDirectories(temp.resolve(name));
    }
}
