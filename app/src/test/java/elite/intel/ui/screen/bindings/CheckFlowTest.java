package elite.intel.ui.screen.bindings;

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
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    /** At the first click: each file a line, a reset named, and the advice for it - nothing about APPLY. */
    @Test
    void theWarningAtTheFirstClickNamesEachFileAndAReset() {
        FileDifference custom = file(true, slot("YawLeftButton", "Key_A", "Key_Q"));
        FileDifference spare = new FileDifference(SPARE, Presence.LIVE_ONLY, false,
                List.of(new LineItem(SPARE, "#file", Kind.FILE, null, null, null, null, "Spare 4.2")));

        String text = CheckFlow.warningText(found(differences(custom, spare), 0), false);

        String files = "- " + getText("bindings.check.file.differs", CUSTOM, 1) + getText("bindings.check.file.reset")
                + "\n- " + getText("bindings.check.file.liveOnly", SPARE);
        assertEquals(getText("bindings.check.intro", files) + "\n\n" + getText("bindings.check.resetAdvice"), text);
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

        String text = CheckFlow.warningText(found(differences, 2), true);

        assertEquals(getText("bindings.check.intro", "- " + getText("bindings.check.file.differs", CUSTOM, 2))
                + "\n\n" + getText("bindings.check.newer.leftOut", NEWER)
                + "\n\n" + getText("bindings.check.edited", 2)
                + "\n\n" + getText("bindings.check.atApply"), text);
    }

    /**
     * The device files at the first click (Storage S9a): each installation that does not match is a line after the
     * files, a reset brings the advice, and one that matches says nothing.
     */
    @Test
    void theWarningNamesEachInstallationThatDiffers() {
        DeviceCheck.Found devices = new DeviceCheck.Found(List.of(
                judged(1, "STEAM", DeviceCheck.State.MATCHES, null),
                judged(2, "EPIC", DeviceCheck.State.RESET_MISSING, null),
                judged(3, "FRONTIER", DeviceCheck.State.UNREADABLE, "locked")));
        BindingsCheck.Found found = new BindingsCheck.Found(new ConfigurationFiles(Map.of(), null, Map.of()),
                Map.of(), differences(), 0, devices);

        String text = CheckFlow.warningText(found, false);

        String installations = "- " + getText("bindings.check.install.resetMissing",
                getText("bindings.firstTimeStartup.place.installation", "EPIC", "Epic"))
                + "\n- " + getText("bindings.check.install.unreadable",
                getText("bindings.firstTimeStartup.place.installation", "FRONTIER", "Frontier"), "locked");
        assertEquals(getText("bindings.check.intro", installations) + "\n\n" + getText("bindings.check.resetAdvice"),
                text);
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

    private static DeviceCheck.Judged judged(long id, String storefront, DeviceCheck.State state, String problem) {
        String folder = storefront.charAt(0) + storefront.substring(1).toLowerCase(Locale.ROOT);
        InstallationRow row = new InstallationRow(id, storefront, folder, false, false);
        return new DeviceCheck.Judged(row, state,
                state == DeviceCheck.State.UNREADABLE ? null : new DeviceFiles(null, Map.of()), problem);
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
                edited, new DeviceCheck.Found(List.of()));
    }
}
