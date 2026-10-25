package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.DeviceDivergence;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.InstallationResult;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Outcome;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the Alias Designer tells the user after a write: installations named as the screen names them, and only the
 * ones that need saying.
 */
class AliasDesignerReportsTest {

    private static final Target STEAM = new Target(1, "STEAM", Path.of("steam"), false);
    private static final Target EPIC = new Target(2, "EPIC", Path.of("epic"), false);

    private final AliasDesignerReports reports = new AliasDesignerReports(Map.of(
            DeviceDivergence.installKey(1), "STEAM",
            DeviceDivergence.installKey(2), "EPIC"));

    @Test
    void installationsAreNamedAsTheScreenNamesThemSorted() {
        assertEquals("EPIC, STEAM", reports.namesOf(List.of(1L, 2L)));
    }

    /**
     * An installation the screen has no name for yet is still named, by its key, rather than dropped.
     */
    @Test
    void anInstallationWithNoNameKeepsItsKey() {
        assertEquals(DeviceDivergence.installKey(9), reports.namesOf(List.of(9L)));
    }

    /**
     * Only an installation that did not take the write gets a line of its own, naming it and why.
     */
    @Test
    void theRevertReportNamesOnlyTheInstallationThatFailed() {
        String text = reports.revertText(new Report(List.of(
                new InstallationResult(STEAM, Outcome.WRITTEN, null, false, List.of()),
                new InstallationResult(EPIC, Outcome.SKIPPED_MISSING, "folder not found: epic", false, List.of()))));

        assertTrue(text.contains("EPIC") && text.contains("folder not found: epic"), text);
        assertFalse(text.contains("STEAM"), text);
        assertEquals(2, text.lines().count(), text);
    }
}
