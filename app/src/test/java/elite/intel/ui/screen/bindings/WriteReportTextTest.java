package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.Backups;
import elite.intel.bindforge.storage.WriteReport;
import elite.intel.bindforge.storage.WriteReport.Kind;
import elite.intel.bindforge.storage.WriteReport.Place;
import elite.intel.bindforge.storage.Written;
import elite.intel.ui.dialog.QuietableNotice;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the user is told after APPLY, RESTORE or first-time startup (storage.md rule 8; principle 9). Only the bindings
 * folder is promised to the next APPLY: an installation a write missed, or one that kept a read-only file, no longer
 * matches the working copy, so APPLY leaves it as it is until the check reconciles it (Alan, 2026-10-27). Compared
 * through the bundles, so it holds in any language.
 */
class WriteReportTextTest {

    private static final Path STEAM = Path.of("Steam");
    private static final Path EPIC = Path.of("Epic");
    private static final Path FRONTIER = Path.of("Frontier");

    @Test
    void anInstallationThatDiffersIsSaidToBeLeftAloneAndPromisedNothing() {
        String text = FirstTimeStartupFlow.placeFailures(report(bindingsUpdated(),
                new Place(Kind.INSTALLATION, "STEAM", STEAM, List.of(), List.of(), List.of(), List.of(), true)));

        assertTrue(text.contains(getText("bindings.applyBar.result.differs", name("STEAM", STEAM))), text);
        assertFalse(text.contains(getText("bindings.firstTimeStartup.result.nextApply")), text);
        assertFalse(text.contains(getText("bindings.firstTimeStartup.result.leftAsItIs", name("STEAM", STEAM))), text);
    }

    @Test
    void anInstallationAWriteMissedIsLeftAsItIsNotPromisedToTheNextApply() {
        String text = FirstTimeStartupFlow.placeFailures(report(bindingsUpdated(),
                new Place(Kind.INSTALLATION, "EPIC", EPIC, List.of(), List.of(), List.of(), List.of("folder not found"))));

        assertTrue(text.contains(getText("bindings.firstTimeStartup.result.place", name("EPIC", EPIC),
                "folder not found")), text);
        assertTrue(text.contains(getText("bindings.firstTimeStartup.result.leftAsItIs", name("EPIC", EPIC))), text);
        assertFalse(text.contains(getText("bindings.firstTimeStartup.result.nextApply")), text);
    }

    @Test
    void aKeptReadOnlyMapIsNamedAndItsInstallationLeftAsItIs() {
        String text = FirstTimeStartupFlow.placeFailures(report(bindingsUpdated(),
                new Place(Kind.INSTALLATION, "FRONTIER", FRONTIER, List.of(), List.of(), List.of("Old.buttonMap"),
                        List.of())));

        assertTrue(text.contains(getText("bindings.firstTimeStartup.result.keptReadOnly", name("FRONTIER", FRONTIER),
                "Old.buttonMap")), text);
        assertTrue(text.contains(getText("bindings.firstTimeStartup.result.leftAsItIs",
                name("FRONTIER", FRONTIER))), text);
        assertFalse(text.contains(getText("bindings.firstTimeStartup.result.nextApply")), text);
    }

    @Test
    void theBindingsFolderAloneIsPromisedToTheNextApply() {
        Place bindings = new Place(Kind.BINDINGS_FOLDER, null, Path.of("Bindings"), List.of(), List.of(), List.of(),
                List.of("Custom.4.2.binds: locked"));

        String text = FirstTimeStartupFlow.placeFailures(report(bindings,
                new Place(Kind.INSTALLATION, "STEAM", STEAM, List.of(), List.of(), List.of(), List.of(), true)));

        assertTrue(text.contains(getText("bindings.firstTimeStartup.result.place",
                getText("bindings.firstTimeStartup.place.bindingsFolder"), "Custom.4.2.binds: locked")), text);
        assertEquals(1, occurrences(text, getText("bindings.firstTimeStartup.result.nextApply")), text);
    }

    @Test
    void everyPlaceUpdatedSaysNothingMore() {
        assertEquals("", FirstTimeStartupFlow.placeFailures(report(bindingsUpdated(),
                new Place(Kind.INSTALLATION, "STEAM", STEAM, List.of("LVWAP.buttonMap"), List.of(), List.of(),
                        List.of()))));
    }

    /** APPLY's report: "updated 1 of 2", the backup, then each place that did not take everything. */
    @Test
    void applysReportNamesTheBackupTheCountAndThePlaces() {
        WriteReport report = report(bindingsUpdated(),
                new Place(Kind.INSTALLATION, "STEAM", STEAM, List.of(), List.of(), List.of(), List.of()),
                new Place(Kind.INSTALLATION, "EPIC", EPIC, List.of(), List.of(), List.of(), List.of(), true));
        Backup backup = new Backup(Path.of("BeforeChanges/2026-10-27_10-00-00.zip"), LocalDateTime.now(),
                Backups.Kind.BEFORE_CHANGES, Backups.Before.APPLY, true, List.of());

        String text = ApplyBar.reportText(new Written(report, backup));

        assertEquals(getText("bindings.applyBar.result", backup.zip().toString(), 1L, 2)
                + FirstTimeStartupFlow.placeFailures(report), text);
        assertTrue(text.contains(getText("bindings.applyBar.result.differs", name("EPIC", EPIC))), text);
    }

    /** Storage S21: only the plain success line can be quieted (Alan, 2026-11-14; storage.md rule 13). */
    @Test
    void applysPlainSuccessCanBeQuieted() {
        Written written = new Written(report(bindingsUpdated(), steamUpdated()), backup());

        CheckFlow.Report report = ApplyBar.report(written, ApplyBar.reportText(written));

        assertEquals(QuietableNotice.BINDFORGE_APPLY_REPORT, report.quietable());
        assertEquals(ApplyBar.reportText(written), report.text());
    }

    /** "Updated 1 of 2" is a failure, and a failure always shows (principle 9). */
    @Test
    void applysReportWithAPlaceNotUpdatedAlwaysShows() {
        Written written = new Written(report(bindingsUpdated(), steamUpdated(),
                new Place(Kind.INSTALLATION, "EPIC", EPIC, List.of(), List.of(), List.of(), List.of("locked"))),
                backup());

        assertNull(ApplyBar.report(written, ApplyBar.reportText(written)).quietable());
    }

    /** News is not a failure, but it is still said: a newer version, a moved device, a file moved away. */
    @Test
    void applysReportWithAnythingMoreAlwaysShows() {
        Written written = new Written(report(bindingsUpdated(), steamUpdated()), backup());

        assertNull(ApplyBar.report(written, ApplyBar.reportText(written) + "\nanything more").quietable());
    }

    @Test
    void firstTimeStartupsPlainSuccessCanBeQuieted() {
        Written written = new Written(report(bindingsUpdated(), steamUpdated()), backup(), false);

        assertEquals(QuietableNotice.BINDFORGE_FIRST_TIME_STARTUP_REPORT,
                FirstTimeStartupFlow.writtenReport(written, 0).quietable());
    }

    @Test
    void firstTimeStartupsReportWithMovesOrTheFlagNotSetAlwaysShows() {
        Written plain = new Written(report(bindingsUpdated(), steamUpdated()), backup(), false);
        Written askedAgain = new Written(report(bindingsUpdated(), steamUpdated()), backup(), true);

        assertNull(FirstTimeStartupFlow.writtenReport(plain, 2).quietable(), "devices moved to a new ID");
        assertNull(FirstTimeStartupFlow.writtenReport(askedAgain, 0).quietable(), "the flag could not be set");
    }

    @Test
    void firstTimeStartupsReportWithAPlaceNotUpdatedAlwaysShows() {
        Written written = new Written(report(bindingsUpdated(),
                new Place(Kind.INSTALLATION, "EPIC", EPIC, List.of(), List.of(), List.of(), List.of(), true)),
                backup(), false);

        assertNull(FirstTimeStartupFlow.writtenReport(written, 0).quietable());
    }

    private static Place steamUpdated() {
        return new Place(Kind.INSTALLATION, "STEAM", STEAM, List.of("LVWAP.buttonMap"), List.of(), List.of(),
                List.of());
    }

    private static Backup backup() {
        return new Backup(Path.of("BeforeChanges/2026-11-14_10-00-00.zip"), LocalDateTime.now(),
                Backups.Kind.BEFORE_CHANGES, Backups.Before.APPLY, true, List.of());
    }

    private static Place bindingsUpdated() {
        return new Place(Kind.BINDINGS_FOLDER, null, Path.of("Bindings"), List.of(), List.of(), List.of(), List.of());
    }

    private static WriteReport report(Place bindings, Place... installations) {
        return new WriteReport(bindings, List.of(installations));
    }

    private static String name(String storefront, Path folder) {
        return getText("bindings.firstTimeStartup.place.installation", storefront, folder);
    }

    private static int occurrences(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }
}
