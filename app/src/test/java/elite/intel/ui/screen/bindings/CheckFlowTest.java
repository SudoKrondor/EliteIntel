package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.BindingsCheck;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.Kind;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.Presence;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.bindforge.storage.ConfigurationFiles;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The check on screen, without Swing (storage.md rule 14; Storage S8R, the S7-S8b review): which answers survive the
 * files changing while the warning was open, and what the warning says. Compared through the bundles, so it holds in
 * any language.
 */
class CheckFlowTest {

    private static final String CUSTOM = "Custom.4.2.binds";
    private static final String SPARE = "Spare.4.2.binds";

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

    /** At APPLY: the files left for a newer version, how many rows the edits change, and that closing cancels APPLY. */
    @Test
    void theWarningAtApplySaysWhatIsLeftOutWhatTheEditsChangeAndThatClosingCancels() {
        FileDifference custom = file(false, slot("YawLeftButton", "Key_A", "Key_Q"),
                slot("RollLeftButton", "Key_E", "Key_R"));
        Differences differences = new Differences(List.of(custom), List.of("Custom.4.3.binds"));

        String text = CheckFlow.warningText(found(differences, 2), true);

        assertEquals(getText("bindings.check.intro", "- " + getText("bindings.check.file.differs", CUSTOM, 2))
                + "\n\n" + getText("bindings.check.leftOut", "Custom.4.3.binds")
                + "\n\n" + getText("bindings.check.edited", 2)
                + "\n\n" + getText("bindings.check.atApply"), text);
    }

    private static LineItem slot(String element, String mine, String theirs) {
        return new LineItem(CUSTOM, element + "/Primary", Kind.SLOT, element, "Primary", null, mine, theirs);
    }

    /** {@code Custom.4.2.binds}, in both places, with {@code items}. */
    private static FileDifference file(boolean reset, LineItem... items) {
        return new FileDifference(CUSTOM, Presence.BOTH, reset, List.of(items));
    }

    private static Differences differences(FileDifference... files) {
        return new Differences(List.of(files), List.of());
    }

    private static BindingsCheck.Found found(Differences differences, int edited) {
        return new BindingsCheck.Found(new ConfigurationFiles(Map.of(), null, Map.of()), Map.of(), differences,
                edited);
    }
}
