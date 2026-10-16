package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.InstallationResult;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Outcome;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which installations missed a write (Alan, 2026-10-15, A14): any miss marks one, and only a push carrying everything
 * Apply carries clears it.
 */
class MissedWritesTest {

    @TempDir
    Path temp;

    private final Map<Long, Boolean> stored = new LinkedHashMap<>();
    private final MissedWrites missedWrites = new MissedWrites(stored::put);

    @Test
    void anInstallationApplySkippedOrFailedIsMarked() {
        missedWrites.afterPush(report(
                result(target(1, false), Outcome.SKIPPED_MISSING),
                result(target(2, false), Outcome.FAILED),
                result(target(3, false), Outcome.WRITTEN)), true);

        assertEquals(Map.of(1L, true, 2L, true), stored, "one that took it, and had missed nothing, is not touched");
    }

    @Test
    void applyLandingOnAMarkedInstallationClearsIt() {
        missedWrites.afterPush(report(
                result(target(1, true), Outcome.WRITTEN),
                result(target(2, true), Outcome.UNCHANGED)), true);

        assertEquals(Map.of(1L, false, 2L, false), stored);
    }

    /** Revert removes nothing, so it can land on an installation still holding a device the others lost. */
    @Test
    void revertLandingClearsNothingButAMissStillMarks() {
        missedWrites.afterPush(report(
                result(target(1, true), Outcome.WRITTEN),
                result(target(2, false), Outcome.FAILED)), false);

        assertEquals(Map.of(2L, true), stored);
    }

    /** Onboarding writes one new element - landing proves nothing about the rest. */
    @Test
    void onboardingMarksAMissAndClearsNothing() {
        missedWrites.afterOnboarding(List.of(
                new DeviceOnboarding.InstallationResult(target(1, false), DeviceOnboarding.Outcome.SKIPPED_MISSING,
                        "folder not found", List.of()),
                new DeviceOnboarding.InstallationResult(target(2, true), DeviceOnboarding.Outcome.WRITTEN, null,
                        List.of())));

        assertEquals(Map.of(1L, true), stored);
    }

    @Test
    void aStoreThatFailsDoesNotStopTheReport() {
        MissedWrites failing = new MissedWrites((id, missed) -> {
            throw new IllegalStateException("database locked");
        });

        failing.afterPush(report(result(target(1, false), Outcome.FAILED)), true);
    }

    /** Found again: missed a write, and reachable now. */
    @Test
    void foundAgainIsMissedAndReachable() throws IOException {
        Target back = reachable(1, true);
        Target fine = reachable(2, false);
        Target stillAway = new Target(3, "FRONTIER", temp.resolve("gone"), false, true);
        Target knownMissing = new Target(4, "OCULUS", Files.createDirectories(temp.resolve("oculus")), true, true);

        assertEquals(List.of(back), MissedWrites.foundAgain(List.of(back, fine, stillAway, knownMissing)));
        assertTrue(MissedWrites.foundAgain(List.of(fine)).isEmpty());
    }

    private Target reachable(long id, boolean missedWrite) throws IOException {
        return new Target(id, "STEAM" + id, Files.createDirectories(temp.resolve("install" + id)), false, missedWrite);
    }

    private Target target(long id, boolean missedWrite) {
        return new Target(id, "STEAM" + id, temp.resolve("install" + id), false, missedWrite);
    }

    private static InstallationResult result(Target target, Outcome outcome) {
        return new InstallationResult(target, outcome, outcome == Outcome.WRITTEN || outcome == Outcome.UNCHANGED
                ? null : "why", false, List.of());
    }

    private static Report report(InstallationResult... results) {
        return new Report(List.of(results));
    }
}
