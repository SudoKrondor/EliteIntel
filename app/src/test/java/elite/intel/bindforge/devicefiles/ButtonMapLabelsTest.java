package elite.intel.bindforge.devicefiles;

import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Generated labels: sized to what the controller has, each one naming its own token. */
class ButtonMapLabelsTest {

    private static final DeviceDescription AXES_UNKNOWN = DeviceDescription.axesUnknown(0);

    /** A stick with its twist on RZ gets X, Y and RZ - not the first three axes by position. */
    @Test
    void aDescribedStickGetsOnlyTheAxesItHas() {
        Map<String, String> labels = ButtonMapLabels.generate(2, 3, described(0, GameAxis.X, GameAxis.Y, GameAxis.RZ));

        assertEquals(List.of("Joy_1", "Joy_2", "Joy_XAxis", "Joy_YAxis", "Joy_RZAxis"), List.copyOf(labels.keySet()));
        assertEquals("Button 2", labels.get("Joy_2"));
        assertEquals("RZ Axis", labels.get("Joy_RZAxis"));
    }

    /** With no description, SDL's count cannot say which axes there are, so every axis token is labelled. */
    @Test
    void anUndescribedStickGetsEveryAxis() {
        Map<String, String> labels = ButtonMapLabels.generate(1, 3, AXES_UNKNOWN);

        assertEquals(List.of("Joy_1", "Joy_XAxis", "Joy_YAxis", "Joy_ZAxis", "Joy_RXAxis", "Joy_RYAxis",
                "Joy_RZAxis", "Joy_UAxis", "Joy_VAxis"), List.copyOf(labels.keySet()));
    }

    /** An axis the game cannot see gets no label - no token could name it. */
    @Test
    void anAxisTheGameCannotSeeGetsNoLabel() {
        DeviceDescription tenAxes = new DeviceDescription(true, List.of(GameAxis.values()), 2, List.of(), 0);

        assertEquals(8, ButtonMapLabels.generate(0, 10, tenAxes).size());
    }

    /**
     * A description that finds no axis where SDL counts three has missed something - a vendor-defined usage, say.
     * A missing label leaves a raw token and an unused one is never shown, so every axis is labelled.
     */
    @Test
    void aDescriptionThatDisagreesWithSdlGetsEveryAxis() {
        assertEquals(8, ButtonMapLabels.generate(0, 3, described(0)).size());
    }

    @Test
    void anUndescribedControllerWithNoAxesGetsNoAxisLabels() {
        assertEquals(List.of("Joy_1"), List.copyOf(ButtonMapLabels.generate(1, 0, AXES_UNKNOWN).keySet()));
    }

    /** Hats come last, four directions each, in the spec's order: up, right, down, left. */
    @Test
    void eachHatGetsFourDirections() {
        Map<String, String> labels = ButtonMapLabels.generate(1, 1, described(2, GameAxis.X));

        assertEquals(List.of("Joy_1", "Joy_XAxis",
                "Joy_POV1Up", "Joy_POV1Right", "Joy_POV1Down", "Joy_POV1Left",
                "Joy_POV2Up", "Joy_POV2Right", "Joy_POV2Down", "Joy_POV2Left"), List.copyOf(labels.keySet()));
        assertEquals("Hat 2 Left", labels.get("Joy_POV2Left"));
    }

    @Test
    void aControllerReportingNothingGetsNoLabels() {
        assertTrue(ButtonMapLabels.generate(0, 0, AXES_UNKNOWN).isEmpty());
        assertTrue(ButtonMapLabels.generate(0, 0, described(0)).isEmpty());
    }

    /** The game shows 18 characters of a label; a 128-button device with four hats stays inside that. */
    @Test
    void labelsFitTheGamesEighteenCharacters() {
        DeviceDescription everything = new DeviceDescription(true, List.of(GameAxis.values()), 0, List.of(), 4);

        ButtonMapLabels.generate(128, 8, everything).values()
                .forEach(label -> assertTrue(label.length() <= 18, label));
    }

    private static DeviceDescription described(int hats, GameAxis... axes) {
        return new DeviceDescription(true, List.of(axes), 0, List.of(), hats);
    }
}
