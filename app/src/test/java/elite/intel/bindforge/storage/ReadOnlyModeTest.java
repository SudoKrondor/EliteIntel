package elite.intel.bindforge.storage;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Read only while an installation is missing (storage.md, <em>An installation missing - read only</em>; Storage S14a):
 * worked out once a run, only once a working copy exists; OVERRIDE turns editing on until Elite-Intel closes; Remove
 * takes a missing installation off the list.
 */
class ReadOnlyModeTest {

    private static final InstallationRow STEAM = new InstallationRow(1, "STEAM", "C:\\Steam", false, false);
    private static final InstallationRow EPIC = new InstallationRow(2, "EPIC", "E:\\Epic", false, true);
    private static final InstallationRow FRONTIER = new InstallationRow(3, "FRONTIER", "F:\\Frontier", false, true);

    private final List<String> announced = new ArrayList<>();
    private final Set<Long> reachable = Set.of(1L);

    private ReadOnlyMode mode(boolean workingCopy, Supplier<List<InstallationRow>> installations) {
        return new ReadOnlyMode(installations, () -> workingCopy, row -> reachable.contains(row.id()),
                () -> announced.add("changed"));
    }

    @Test
    void anInstallationMissingMakesItReadOnlyAndIsNamed() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM, EPIC));

        assertTrue(mode.active());
        assertEquals(List.of(EPIC), mode.missing());
        assertEquals(List.of("changed"), announced, "the screens hear it once it is worked out");
    }

    @Test
    void everyInstallationThereIsNotReadOnly() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM));

        assertFalse(mode.active());
        assertEquals(List.of(), mode.missing());
    }

    /** Q2 (Alan, 2026-11-05): before first-time startup has ever completed - no working copy - it does not apply. */
    @Test
    void withNoWorkingCopyYetItDoesNotApply() {
        ReadOnlyMode mode = mode(false, () -> List.of(STEAM, EPIC));

        assertFalse(mode.active());
        assertEquals(List.of(), mode.missing());
    }

    /** Nothing checks again while Elite-Intel runs. */
    @Test
    void itIsWorkedOutOnceARun() {
        int[] reads = {0};
        ReadOnlyMode mode = mode(true, () -> {
            reads[0]++;
            return List.of(STEAM, EPIC);
        });

        mode.active();
        mode.missing();
        mode.active();

        assertEquals(1, reads[0]);
        assertEquals(List.of("changed"), announced);
    }

    /**
     * Until it is worked out, it reads as not read only.
     */
    @Test
    void beforeItIsWorkedOutTheScreensSeeNothing() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM, EPIC));

        assertFalse(mode.activeIfKnown());
        assertEquals(List.of(), mode.missingIfKnown());

        mode.missing();

        assertTrue(mode.activeIfKnown());
        assertEquals(List.of(EPIC), mode.missingIfKnown());
    }

    /**
     * The defect this pins: the work-out held the monitor the EDT's readers take, so a stalled drive froze the window.
     * Remove runs on the EDT too, and must neither wait nor be lost: the work-out may have read the row before it went.
     */
    @Test
    void whileItIsBeingWorkedOutTheEdtNeverWaitsAndARemoveCounts() throws InterruptedException {
        CountDownLatch working = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ReadOnlyMode mode = mode(true, () -> {
            working.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return List.of(STEAM, EPIC, FRONTIER);
        });
        Thread workOut = Thread.ofPlatform().start(mode::missing);
        assertTrue(working.await(5, TimeUnit.SECONDS));

        try {
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                assertFalse(mode.activeIfKnown());
                assertEquals(List.of(), mode.missingIfKnown());
                mode.removed(EPIC.id());
            });
        } finally {
            release.countDown();
        }
        workOut.join();

        assertEquals(List.of(FRONTIER), mode.missing(), "the row removed while it was worked out stays out");
    }

    @Test
    void overrideTurnsEditingOnAndStillNamesTheMissing() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM, EPIC));
        mode.active();

        mode.override();
        mode.override();

        assertFalse(mode.active());
        assertFalse(mode.activeIfKnown());
        assertEquals(List.of(EPIC), mode.missing());
        assertEquals(List.of("changed", "changed"), announced, "a second override changes nothing");
    }

    /** Q4 (Alan, 2026-11-05): Remove takes it off the missing list; read only ends when none is left. */
    @Test
    void removingTheMissingInstallationsEndsReadOnly() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM, EPIC, FRONTIER));
        mode.active();

        mode.removed(EPIC.id());
        assertTrue(mode.active(), "Frontier is still missing");
        assertEquals(List.of(FRONTIER), mode.missing());

        mode.removed(FRONTIER.id());
        assertFalse(mode.active());
    }

    @Test
    void removingAnInstallationThatIsThereChangesNothing() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM, EPIC));
        mode.active();

        mode.removed(STEAM.id());

        assertEquals(List.of(EPIC), mode.missing());
        assertEquals(List.of("changed"), announced);
    }

    /** A fault working it out must not lock the user out of BindForge; every write still backs up first. */
    @Test
    void aFaultWorkingItOutIsNotReadOnly() {
        ReadOnlyMode mode = mode(true, () -> {
            throw new IllegalStateException("database closed");
        });

        assertFalse(mode.active());
        assertEquals(List.of(), mode.missing());
    }

    /**
     * Q6 (Alan, 2026-11-05): the warning at start names what is missing, and is said once a run.
     */
    @Test
    void theWarningNamesTheMissingOnceARun() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM, EPIC));
        List<List<InstallationRow>> warned = new ArrayList<>();

        mode.warnOnce(warned::add, Runnable::run);
        mode.warnOnce(warned::add, Runnable::run);

        assertEquals(List.of(List.of(EPIC)), warned);
    }

    @Test
    void withNothingMissingThereIsNoWarning() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM));
        List<List<InstallationRow>> warned = new ArrayList<>();

        mode.warnOnce(warned::add, Runnable::run);

        assertEquals(List.of(), warned);
    }

    @Test
    void onceOverriddenThereIsNoWarning() {
        ReadOnlyMode mode = mode(true, () -> List.of(STEAM, EPIC));
        List<List<InstallationRow>> warned = new ArrayList<>();
        mode.active();
        mode.override();

        mode.warnOnce(warned::add, Runnable::run);

        assertEquals(List.of(), warned);
    }

    @Test
    void aWorkingCopyThatCannotBeLookedForIsNotReadOnly() {
        ReadOnlyMode mode = new ReadOnlyMode(() -> List.of(STEAM, EPIC), () -> {
            throw new IOException("interrupted write could not be finished");
        }, row -> reachable.contains(row.id()), () -> announced.add("changed"));

        assertFalse(mode.active());
    }
}
