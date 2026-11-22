package elite.intel.bindforge.devices;

import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Device Editor's boxes: built from what the controller reports, each holding a label, saved whole. */
class DeviceEditorInputsTest {

    /** A stick with its twist on RZ shows X, Y and RZ - the axes it has, never the first three by position. */
    @Test
    void anAttachedStickOffersTheAxesItHasAndEveryButtonAndHat() {
        DeviceDescription twistOnRz = new DeviceDescription(true, List.of(GameAxis.X, GameAxis.Y, GameAxis.RZ), 2,
                List.of(GameAxis.X, GameAxis.Y, GameAxis.RZ), 1);

        DeviceEditorInputs inputs = DeviceEditorInputs.ofController(3, 5, twistOnRz);

        assertTrue(inputs.attached());
        assertTrue(inputs.axesKnown());
        assertEquals(List.of("Joy_XAxis", "Joy_YAxis", "Joy_RZAxis"), inputs.axes());
        assertEquals(2, inputs.axesWithoutSlot(), "the editor says how many the game cannot use");
        assertEquals(List.of("Joy_1", "Joy_2", "Joy_3"), inputs.buttons());
        assertEquals(List.of("Joy_XAxis", "Joy_YAxis", "Joy_RZAxis", "Joy_1", "Joy_2", "Joy_3",
                "Joy_POV1Up", "Joy_POV1Right", "Joy_POV1Down", "Joy_POV1Left"), inputs.tokens());
    }

    /** Where the axes cannot be read, all eight are offered, as generation labels them (Alan, 2026-10-10). */
    @Test
    void axesThatCannotBeReadAreAllEight() {
        DeviceEditorInputs inputs = DeviceEditorInputs.ofController(0, 3, DeviceDescription.axesUnknown(0));

        assertFalse(inputs.axesKnown());
        assertEquals(8, inputs.axes().size());
        assertEquals(0, inputs.axesWithoutSlot());
    }

    /** A description counting no axis where SDL counts two has missed something, and is not trusted. */
    @Test
    void aDescriptionDisagreeingWithSdlIsTreatedAsUnknown() {
        DeviceDescription none = new DeviceDescription(true, List.of(), 0, List.of(), 0);

        DeviceEditorInputs inputs = DeviceEditorInputs.ofController(0, 2, none);

        assertFalse(inputs.axesKnown());
        assertEquals(8, inputs.axes().size());
    }

    /** Pedals with three axes and nothing else have no buttons or hats to show. */
    @Test
    void aControllerWithNoButtonsOffersNone() {
        DeviceDescription pedals = new DeviceDescription(true, List.of(GameAxis.X, GameAxis.Y, GameAxis.RZ), 0,
                List.of(), 0);

        DeviceEditorInputs inputs = DeviceEditorInputs.ofController(0, 3, pedals);

        assertTrue(inputs.buttons().isEmpty());
        assertEquals(0, inputs.hats());
        assertFalse(inputs.isEmpty());
    }

    /**
     * A controller not attached cannot report its inputs, so it offers what its labels name - buttons in the game's
     * numbering, not by text, and a hat whole. Half-axis labels name no box.
     */
    @Test
    void anUnattachedControllerOffersTheInputsItsLabelsName() {
        Map<String, String> saved = labels("Joy_10", "TEN", "Joy_2", "TWO", "Joy_RZAxis", "TWIST",
                "Joy_POV2Left", "HAT LEFT", "Pos_Joy_XAxis", "RIGHT HALF", "Joy_0", "NOT A BUTTON");

        DeviceEditorInputs inputs = DeviceEditorInputs.ofSavedLabels(saved);

        assertFalse(inputs.attached());
        assertEquals(List.of("Joy_2", "Joy_10"), inputs.buttons());
        assertEquals(List.of("Joy_RZAxis"), inputs.axes());
        assertEquals(2, inputs.hats(), "every hat up to the highest labelled");
    }

    /**
     * Review, 2026-10-11: a damaged or hand-edited label naming hat 500 must not build two thousand boxes, and one
     * past {@code int} must not throw. Neither names a hat the game can bind.
     */
    @Test
    void aHatPastWhatTheGameCanBindOffersNoBox() {
        DeviceEditorInputs inputs = DeviceEditorInputs.ofSavedLabels(
                labels("Joy_POV500Up", "FAR", "Joy_POV99999999999Left", "FARTHER", "Joy_POV4Down", "LAST"));

        assertEquals(4, inputs.hats(), "up to the fourth, which DirectInput can report");
        assertEquals("Joy_POV99999999999Left", DeviceEditorInputs.defaultLabel("Joy_POV99999999999Left"));
    }

    @Test
    void anUnattachedControllerWithNoLabelsOffersNothing() {
        assertTrue(DeviceEditorInputs.ofSavedLabels(Map.of()).isEmpty());
    }

    /** A box never starts empty: the saved label, or the generated default. */
    @Test
    void everyBoxStartsWithItsSavedLabelOrTheDefault() {
        DeviceEditorInputs inputs = DeviceEditorInputs.ofController(2, 1,
                new DeviceDescription(true, List.of(GameAxis.Z), 0, List.of(GameAxis.Z), 1));

        Map<String, String> starting = inputs.startingLabels(labels("Joy_1", "TRIGGER", "Joy_2", "  "));

        assertEquals("Z Axis", starting.get("Joy_ZAxis"));
        assertEquals("TRIGGER", starting.get("Joy_1"));
        assertEquals("Button 2", starting.get("Joy_2"), "a blank saved label is no label");
        assertEquals("Hat 1 Right", starting.get("Joy_POV1Right"));
    }

    @Test
    void aBoxIsTrimmedCutToEighteenAndClearingItPutsTheDefaultBack() {
        assertEquals("PINKY", DeviceEditorInputs.clean("Joy_4", "  PINKY  "));
        assertEquals("ABCDEFGHIJKLMNOPQR", DeviceEditorInputs.clean("Joy_4", "ABCDEFGHIJKLMNOPQRSTU"));
        assertEquals("Button 4", DeviceEditorInputs.clean("Joy_4", "   "));
        assertEquals("Hat 3 Down", DeviceEditorInputs.clean("Joy_POV3Down", ""));
    }

    /** Every box is written, defaults included, and a saved label with no box is kept unchanged. */
    @Test
    void savingWritesEveryBoxAndKeepsLabelsWithNoBox() {
        Map<String, String> saved = labels("Joy_1", "OLD", "Pos_Joy_XAxis", "RIGHT HALF", "Joy_40", "FORTY");
        Map<String, String> boxes = labels("Joy_1", "TRIGGER ", "Joy_2", "Button 2", "Joy_XAxis", "");

        Map<String, String> written = DeviceEditorInputs.toSave(saved, boxes, false);

        assertEquals(labels("Joy_1", "TRIGGER", "Pos_Joy_XAxis", "RIGHT HALF", "Joy_40", "FORTY",
                "Joy_2", "Button 2", "Joy_XAxis", "X Axis"), written);
    }

    /** On a device the game draws itself, a default nobody touched is never written over an icon. */
    @Test
    void onADeviceTheGameDrawsOnlyEditedOrSavedLabelsAreWritten() {
        Map<String, String> saved = labels("Joy_3", "[x52b3]");
        Map<String, String> boxes = labels("Joy_1", "Button 1", "Joy_2", "FIRE", "Joy_3", "[x52b3]");

        Map<String, String> written = DeviceEditorInputs.toSave(saved, boxes, true);

        assertEquals(labels("Joy_3", "[x52b3]", "Joy_2", "FIRE"), written);
    }

    @Test
    void aChangeIsJudgedAsSaveWouldKeepIt() {
        Map<String, String> starting = labels("Joy_1", "Button 1", "Joy_2", "TRIGGER");

        assertFalse(DeviceEditorInputs.changed(starting, labels("Joy_1", "", "Joy_2", "TRIGGER  ")));
        assertTrue(DeviceEditorInputs.changed(starting, labels("Joy_1", "Button 1", "Joy_2", "FIRE")));
    }

    private static Map<String, String> labels(String... tokenThenLabel) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < tokenThenLabel.length; i += 2) labels.put(tokenThenLabel[i], tokenThenLabel[i + 1]);
        return labels;
    }
}
