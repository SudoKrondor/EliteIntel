package elite.intel.bindforge.devicefiles;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Generated labels: sized to what the controller reports, each one naming its own token. */
class ButtonMapLabelsTest {

    @Test
    void buttonsComeFirstThenEveryAxis() {
        Map<String, String> labels = ButtonMapLabels.generate(2, 3);

        assertEquals(List.of("Joy_1", "Joy_2", "Joy_XAxis", "Joy_YAxis", "Joy_ZAxis", "Joy_RXAxis", "Joy_RYAxis",
                "Joy_RZAxis", "Joy_UAxis", "Joy_VAxis"), List.copyOf(labels.keySet()));
        assertEquals("Button 2", labels.get("Joy_2"));
    }

    /**
     * SDL numbers a stick's X, Y and RZ as axes 0, 1 and 2, so labelling by position would leave the twist raw.
     * Every axis token is labelled instead, and U and V are past ButtonInputMapper's axis-6 limit.
     */
    @Test
    void aStickWithATwistOnRzGetsItsTwistLabelled() {
        Map<String, String> labels = ButtonMapLabels.generate(0, 3);

        assertEquals("RZ Axis", labels.get("Joy_RZAxis"));
        assertEquals("V Axis", labels.get("Joy_VAxis"));
    }

    /** The game names no axis past V. */
    @Test
    void anAxisPastTheEighthAddsNoLabel() {
        assertEquals(8, ButtonMapLabels.generate(0, 11).size());
    }

    @Test
    void aControllerWithNoAxesGetsNoAxisLabels() {
        assertEquals(List.of("Joy_1"), List.copyOf(ButtonMapLabels.generate(1, 0).keySet()));
    }

    @Test
    void aControllerReportingNothingGetsNoLabels() {
        assertTrue(ButtonMapLabels.generate(0, 0).isEmpty());
    }

    /** The game shows 18 characters of a label; a 128-button device stays inside that. */
    @Test
    void labelsFitTheGamesEighteenCharacters() {
        ButtonMapLabels.generate(128, 8).values()
                .forEach(label -> assertTrue(label.length() <= 18, label));
    }
}
