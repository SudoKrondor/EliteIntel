package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.Backups;
import elite.intel.bindforge.storage.BindingsCheck;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.Kind;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.NewerVersion;
import elite.intel.bindforge.storage.BindingsComparison.Presence;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.DeviceCheck;
import elite.intel.bindforge.storage.DeviceFiles;
import elite.intel.bindforge.storage.Leftovers;
import elite.intel.bindforge.storage.PlainBinds;
import elite.intel.bindforge.storage.WriteReport;
import elite.intel.bindforge.storage.WriteReport.Place;
import elite.intel.bindforge.storage.Written;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.ui.screen.bindings.CheckWarningDialog.Tone;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The check on screen, without Swing (storage.md rule 14; Storage S8R, the S7-S8b review): which answers survive the
 * files changing while the warning was open, and what the warning says. Compared through the bundles, so it holds in
 * any language. A newer version's question and report (Storage S10).
 */
class CheckFlowTest {

    private static final String CUSTOM = "Custom.4.2.binds";
    private static final String SPARE = "Spare.4.2.binds";
    private static final String NEWER = "Custom.4.3.binds";

    /**
     * The answers kept on every row that did not change (storage.md, <em>Settled with Storage S8's sources</em>): a row
     * still the same keeps its answer; one whose value changed, or that no longer differs, loses it.
     */
    @Test
    void onlyTheAnswersToRowsThatDidNotChangeAreKept() {
        LineItem yaw = slot("YawLeftButton", "Key_A", "Key_Q");
        LineItem roll = slot("RollLeftButton", "Key_E", "Key_R");
        LineItem pitch = slot("PitchUpButton", "Key_W", "Key_S");
        LineItem rollNow = slot("RollLeftButton", "Key_E", "Key_T");
        BindingsCheck.Found before = found(differences(file(false, yaw, roll, pitch)), 0);
        BindingsCheck.Found now = found(differences(file(false, yaw, rollNow)), 0);

        Map<String, Side> kept = CheckFlow.kept(before, now,
                Map.of(yaw.id(), Side.LIVE, roll.id(), Side.WORKING_COPY, pitch.id(), Side.LIVE));

        assertEquals(Map.of(yaw.id(), Side.LIVE), kept);
    }

    /**
     * At the first click: each file a row, a reset named and in warning colour, and the advice for it below the grid -
     * nothing about APPLY (the grid, 2026-11-10).
     */
    @Test
    void theWarningAtTheFirstClickNamesEachFileAndAReset() {
        FileDifference custom = file(true, slot("YawLeftButton", "Key_A", "Key_Q"));
        FileDifference spare = new FileDifference(SPARE, Presence.LIVE_ONLY, false,
                List.of(new LineItem(SPARE, "#file", Kind.FILE, null, null, null, null, "Spare 4.2")));

        CheckWarningDialog.Warning warning = CheckFlow.warning(found(differences(custom, spare), 0), false);

        assertEquals(getText("bindings.check.intro"), warning.intro());
        assertEquals(List.of(
                row(CUSTOM, getText("bindings.check.found.reset", getText("bindings.check.found.differs", 1)),
                        "bindings.check.fate.choice", Tone.WARNING),
                row(SPARE, getText("bindings.check.found.liveOnly"), "bindings.check.fate.choice", Tone.NORMAL)),
                warning.rows());
        assertEquals(getText("bindings.check.choose") + "\n\n" + getText("bindings.check.resetAdvice"),
                warning.after());
    }

    /**
     * At APPLY: a newer version that cannot be read, left as it is; how many rows the edits change; and that closing
     * cancels APPLY.
     */
    @Test
    void theWarningAtApplySaysWhatIsLeftOutWhatTheEditsChangeAndThatClosingCancels() {
        FileDifference custom = file(false, slot("YawLeftButton", "Key_A", "Key_Q"),
                slot("RollLeftButton", "Key_E", "Key_R"));
        Differences differences = new Differences(List.of(custom), List.of(), List.of(NEWER));

        CheckWarningDialog.Warning warning = CheckFlow.warning(found(differences, 2), true);

        assertEquals(List.of(row(CUSTOM, getText("bindings.check.found.differs", 2), "bindings.check.fate.choice",
                Tone.NORMAL)), warning.rows());
        assertEquals(getText("bindings.check.choose")
                + "\n\n" + getText("bindings.check.newer.leftOut", NEWER)
                + "\n\n" + getText("bindings.check.edited", 2)
                + "\n\n" + getText("bindings.check.atApply"), warning.after());
    }

    /**
     * The device files at the first click (Storage S9a): each installation that does not match is a row after the
     * files, in danger colour when its file is gone or unreadable; a missing file's note and the reset advice are said
     * below; one that matches has no row.
     */
    @Test
    void theWarningNamesEachInstallationThatDiffers() {
        DeviceCheck.Found devices = new DeviceCheck.Found(List.of(
                judged(1, "STEAM", DeviceCheck.State.MATCHES, null),
                judged(2, "EPIC", DeviceCheck.State.RESET_MISSING, null),
                judged(3, "FRONTIER", DeviceCheck.State.UNREADABLE, "locked")));
        BindingsCheck.Found found = new BindingsCheck.Found(new ConfigurationFiles(Map.of(), null, Map.of()),
                Map.of(), differences(), 0, devices, Leftovers.Found.NONE, List.of());

        CheckWarningDialog.Warning warning = CheckFlow.warning(found, false);

        assertEquals(List.of(
                row(getText("bindings.firstTimeStartup.place.installation", "EPIC", "Epic"),
                        getText("bindings.check.found.install.resetMissing"), "bindings.check.fate.putBack",
                        Tone.DANGER),
                row(getText("bindings.firstTimeStartup.place.installation", "FRONTIER", "Frontier"),
                        getText("bindings.check.found.install.unreadable", "locked"), "bindings.check.fate.leftAlone",
                        Tone.DANGER)), warning.rows());
        assertEquals(getText("bindings.check.note.resetMissing") + "\n\n" + getText("bindings.check.choose")
                + "\n\n" + getText("bindings.check.resetAdvice"), warning.after());
    }

    /**
     * A newer version's question (Storage S10): every binding carried over asks to approve, naming the new slots; at
     * APPLY it says closing cancels APPLY.
     */
    @Test
    void aNewerVersionWithEveryBindingCarriedOverAsksToApproveAndNamesTheNewSlots() {
        NewerVersion version = new NewerVersion(CUSTOM, NEWER, false, List.of("NewThing", "OtherThing"), List.of());

        assertEquals(getText("bindings.check.newer.carriedOver", NEWER, CUSTOM, 2, "NewThing, OtherThing")
                + "\n\n" + getText("bindings.check.atApply"), CheckFlow.newerText(version, true));
    }

    /** Lines that differ: how many, the new slots - none here, so said - and a reset's advice. */
    @Test
    void aNewerVersionWithLinesThatDifferSaysHowManyAndAReset() {
        LineItem row = new LineItem(NEWER, "YawLeftButton/Primary", Kind.SLOT, "YawLeftButton", "Primary", null,
                "Key_A", "Key_Q");
        NewerVersion version = new NewerVersion(CUSTOM, NEWER, true, List.of(), List.of(row));

        assertEquals(getText("bindings.check.newer.changed", NEWER, CUSTOM, 1, 0,
                getText("bindings.check.newer.noNewSlots")) + "\n\n" + getText("bindings.check.resetAdvice"),
                CheckFlow.newerText(version, false));
    }

    /** The report names each newer version written and its new slots to bind. */
    @Test
    void theReportNamesEachNewerVersionWritten() {
        NewerVersion version = new NewerVersion(CUSTOM, NEWER, false, List.of("NewThing"), List.of());
        Differences differences = new Differences(List.of(), List.of(version), List.of());

        assertEquals("\n" + getText("bindings.check.newer.result", NEWER, CUSTOM, "NewThing"),
                CheckFlow.newerReport(found(differences, 0)));
    }

    /** A newer version's answers are kept like any row's when the files change meanwhile. */
    @Test
    void theAnswersToANewerVersionsRowsAreKeptToo() {
        LineItem row = new LineItem(NEWER, "YawLeftButton/Primary", Kind.SLOT, "YawLeftButton", "Primary", null,
                "Key_A", "Key_Q");
        Differences differences = new Differences(List.of(),
                List.of(new NewerVersion(CUSTOM, NEWER, false, List.of(), List.of(row))), List.of());
        BindingsCheck.Found found = found(differences, 0);

        assertEquals(Map.of(row.id(), Side.WORKING_COPY),
                CheckFlow.kept(found, found, Map.of(row.id(), Side.WORKING_COPY)));
    }

    // ---- what does not belong in the bindings folder (Storage S13b, S13d)

    /**
     * Only files to move: the warning that offers Tidy up - its own intro, a row each in name order, a plain binds
     * file's note said once below - and at APPLY, that closing cancels it.
     */
    @Test
    void aWarningHoldingOnlyFilesToMoveSaysTheyAreTidiedUp() {
        BindingsCheck.Found found = withLeftovers(differences());

        CheckWarningDialog.Warning warning = CheckFlow.warning(found, true);

        assertTrue(CheckFlow.onlyLeftovers(found));
        assertEquals(getText("bindings.check.leftover.intro"), warning.intro());
        assertEquals(List.of(
                row("Broken.binds", getText("bindings.check.found.leftover"), "bindings.check.fate.moved", Tone.NORMAL),
                row("Spare.binds", getText("bindings.check.found.plain"), "bindings.check.fate.moved", Tone.NORMAL)),
                warning.rows());
        assertEquals(getText("bindings.check.note.plain") + "\n\n" + getText("bindings.check.leftover.tidyUp")
                + "\n\n" + getText("bindings.check.atApply"), warning.after());
    }

    /** With a row too, the usual intro and choices; the files to move are rows after the files. */
    @Test
    void filesToMoveAreRowsInTheUsualWarningWhenAFileDiffersToo() {
        BindingsCheck.Found found = withLeftovers(differences(file(false, slot("YawLeftButton", "Key_A", "Key_Q"))));

        CheckWarningDialog.Warning warning = CheckFlow.warning(found, false);

        assertFalse(CheckFlow.onlyLeftovers(found));
        assertEquals(getText("bindings.check.intro"), warning.intro());
        assertEquals(List.of(
                row(CUSTOM, getText("bindings.check.found.differs", 1), "bindings.check.fate.choice", Tone.NORMAL),
                row("Broken.binds", getText("bindings.check.found.leftover"), "bindings.check.fate.moved", Tone.NORMAL),
                row("Spare.binds", getText("bindings.check.found.plain"), "bindings.check.fate.moved", Tone.NORMAL)),
                warning.rows());
        assertEquals(getText("bindings.check.note.plain") + "\n\n" + getText("bindings.check.choose"),
                warning.after());
    }

    /** A stray file alone is something to ask about: the check offers Tidy up, never only names it (Storage S13d). */
    @Test
    void aStrayFileAloneIsSomethingToAsk() {
        BindingsCheck.Found found = found(differences(), new Leftovers.Found(Map.of(), List.of("notes.txt")));

        assertTrue(found.anythingToAsk());
        assertTrue(CheckFlow.onlyLeftovers(found));
    }

    /**
     * The report (D4): what was moved and where, and each plain file moved, and why. One that could not be moved has
     * no line here - the failure line names it.
     */
    @Test
    void theReportNamesWhatWasMovedAndWhy() {
        Path to = Path.of("Leftover User Files", "2026-11-08_10-00-00");
        Place bindings = new Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"),
                List.of("Spare.4.2.binds"), List.of(), List.of(), List.of("Spare.binds: not moved"),
                false, List.of("Broken.binds", "custom.binds"), to);

        String report = CheckFlow.leftoversReport(written(bindings));

        assertEquals("\n" + getText("bindings.check.result.moved", "Broken.binds, custom.binds", to.toString())
                + "\n" + getText("bindings.check.result.plainTwin", "custom.binds", CUSTOM), report);
        assertFalse(report.contains(getText("bindings.check.result.plainConverted", "Spare.binds", "Spare.4.2.binds")),
                "a converted file that was not moved has no line of its own (Storage S13eR2, D3)");
    }

    /** A plain file whose kept lines went into its twin names the twin (Storage S13e). */
    @Test
    void theReportNamesTheTwinAPlainFilesLinesWentInto() {
        Path to = Path.of("Leftover User Files", "2026-11-08_10-00-00");
        Place bindings = new Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"),
                List.of("Live.4.2.binds"), List.of(), List.of(), List.of(), false, List.of("Live.binds"), to);

        assertEquals("\n" + getText("bindings.check.result.moved", "Live.binds", to.toString())
                        + "\n" + getText("bindings.check.result.plainMerged", "Live.binds", "Live.4.2.binds"),
                CheckFlow.leftoversReport(written(bindings)));
    }

    /** A plain file whose twin the write took away says nothing was taken from it (the 2026-11-09 review). */
    @Test
    void theReportSaysAPlainFileWhoseTwinLeftGaveNothing() {
        Path to = Path.of("Leftover User Files", "2026-11-09_10-00-00");
        Place bindings = new Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"),
                List.of(), List.of("Gone.4.2.binds"), List.of(), List.of(), false, List.of("Gone.binds"), to);

        assertEquals("\n" + getText("bindings.check.result.moved", "Gone.binds", to.toString())
                        + "\n" + getText("bindings.check.result.plainTwinLeaves", "Gone.binds", "Gone.4.2.binds"),
                CheckFlow.leftoversReport(written(bindings)));
    }

    /**
     * A plain file holding lines its twin lacks (Storage S13e): its own warning row, naming how many and the twin, and
     * its note below - not the plain note, which is for a plain file with no lines to choose; its line items are the
     * warning's, after the binds files', so the warning offers every choice, not Tidy up.
     */
    @Test
    void aPlainFileWithRowsHasItsOwnLineAndRows() {
        LineItem row = new LineItem("Custom.binds", "YawLeftButton/Primary", Kind.SLOT, "YawLeftButton", "Primary",
                null, "Key_A", "Key_Z");
        PlainBinds.Rows rows = new PlainBinds.Rows(CUSTOM, new FileDifference("Custom.binds", Presence.BOTH, false,
                List.of(row)));
        BindingsCheck.Found found = new BindingsCheck.Found(new ConfigurationFiles(Map.of(), null, Map.of()), Map.of(),
                differences(), 0, new DeviceCheck.Found(List.of()),
                new Leftovers.Found(Map.of("Custom.binds", new byte[]{1}), List.of()), List.of(rows));

        assertEquals(List.of(row("Custom.binds", getText("bindings.check.found.plainRows", 1, CUSTOM),
                "bindings.check.fate.choiceThenMoved", Tone.NORMAL)), CheckFlow.leftoverRows(found));
        assertEquals(List.of(getText("bindings.check.note.plainRows")), CheckFlow.notes(found));
        assertEquals(List.of(row), found.items());
        assertTrue(found.anyRows());
        assertFalse(CheckFlow.onlyLeftovers(found), "rows to answer - every choice, not Tidy up");
    }

    /** An answer to a plain file's row that did not change is kept when the files changed meanwhile. */
    @Test
    void anAnswerToAPlainFilesRowIsKept() {
        LineItem row = new LineItem("Custom.binds", "YawLeftButton/Primary", Kind.SLOT, "YawLeftButton", "Primary",
                null, "Key_A", "Key_Z");
        BindingsCheck.Found found = new BindingsCheck.Found(new ConfigurationFiles(Map.of(), null, Map.of()), Map.of(),
                differences(), 0, new DeviceCheck.Found(List.of()),
                new Leftovers.Found(Map.of("Custom.binds", new byte[]{1}), List.of()),
                List.of(new PlainBinds.Rows(CUSTOM, new FileDifference("Custom.binds", Presence.BOTH, false,
                        List.of(row)))));

        assertEquals(Map.of(row.id(), Side.LIVE), CheckFlow.kept(found, found, Map.of(row.id(), Side.LIVE)));
    }

    /** A read-only binds file the write would have removed is kept, and named in the kept-read-only line. */
    @Test
    void aReadOnlyBindsFileKeptIsNamed() {
        Place bindings = new Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"), List.of(), List.of(),
                List.of("Live.4.2.binds"), List.of());

        assertEquals("\n" + getText("bindings.check.result.keptReadOnly", "Live.4.2.binds"),
                CheckFlow.keptReadOnly(written(bindings)));
    }

    @Test
    void nothingKeptAndNothingMovedSaysNothing() {
        Place bindings = new Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"), List.of(), List.of(),
                List.of(), List.of());

        assertEquals("", CheckFlow.keptReadOnly(written(bindings)));
        assertEquals("", CheckFlow.leftoversReport(written(bindings)));
    }

    /**
     * The check's write with {@code bindings}: {@code custom.binds} a twin, {@code KeyboardMouseOnly.binds} a factory
     * file, {@code Spare.binds} converted, {@code Live.binds} merged into its twin (Storage S13e), and
     * {@code Broken.binds} and {@code notes.txt} moved as they are.
     */
    private static Written written(Place bindings) {
        PlainBinds.Plan plain = new PlainBinds.Plan(List.of(
                new PlainBinds.Tidied("KeyboardMouseOnly.binds", PlainBinds.Fate.FACTORY, "KeyboardMouseOnly"),
                new PlainBinds.Tidied("Spare.binds", PlainBinds.Fate.CONVERTED, "Spare.4.2.binds"),
                new PlainBinds.Tidied("custom.binds", PlainBinds.Fate.TWIN, CUSTOM),
                new PlainBinds.Tidied("Live.binds", PlainBinds.Fate.MERGED, "Live.4.2.binds"),
                new PlainBinds.Tidied("Gone.binds", PlainBinds.Fate.TWIN_LEAVES, "Gone.4.2.binds")),
                Map.of("Spare.4.2.binds", new byte[]{1}), Map.of("Live.4.2.binds", new byte[]{2}));
        return new Written(new WriteReport(bindings, List.of()), new Backup(Path.of("backup.zip"),
                LocalDateTime.now(), Backups.Kind.BEFORE_CHANGES, Backups.Before.CHECK, true, List.of()),
                new Leftovers.Plan(plain, List.of("Broken.binds", "notes.txt")));
    }

    /** {@code differences}, with {@code Spare.binds} to judge and {@code Broken.binds}, which does not read. */
    private static BindingsCheck.Found withLeftovers(Differences differences) {
        return found(differences, new Leftovers.Found(Map.of("Spare.binds", new byte[]{1}), List.of("Broken.binds")));
    }

    /** {@code differences} at the first click - no edits - with {@code leftovers}. */
    private static BindingsCheck.Found found(Differences differences, Leftovers.Found leftovers) {
        return new BindingsCheck.Found(new ConfigurationFiles(Map.of(), null, Map.of()), Map.of(), differences,
                0, new DeviceCheck.Found(List.of()), leftovers, List.of());
    }

    private static DeviceCheck.Judged judged(long id, String storefront, DeviceCheck.State state, String problem) {
        String folder = storefront.charAt(0) + storefront.substring(1).toLowerCase(Locale.ROOT);
        InstallationRow row = new InstallationRow(id, storefront, folder, false, false);
        return new DeviceCheck.Judged(row, state,
                state == DeviceCheck.State.UNREADABLE ? null : new DeviceFiles(null, Map.of()), problem);
    }

    /** A grid row, its fate given by bundle key. */
    private static CheckWarningDialog.Row row(String file, String found, String fateKey, Tone tone) {
        return new CheckWarningDialog.Row(file, found, getText(fateKey), tone);
    }

    private static LineItem slot(String element, String mine, String theirs) {
        return new LineItem(CUSTOM, element + "/Primary", Kind.SLOT, element, "Primary", null, mine, theirs);
    }

    /** {@code Custom.4.2.binds}, in both places, with {@code items}. */
    private static FileDifference file(boolean reset, LineItem... items) {
        return new FileDifference(CUSTOM, Presence.BOTH, reset, List.of(items));
    }

    private static Differences differences(FileDifference... files) {
        return new Differences(List.of(files), List.of(), List.of());
    }

    private static BindingsCheck.Found found(Differences differences, int edited) {
        return new BindingsCheck.Found(new ConfigurationFiles(Map.of(), null, Map.of()), Map.of(), differences,
                edited, new DeviceCheck.Found(List.of()), Leftovers.Found.NONE, List.of());
    }
}
