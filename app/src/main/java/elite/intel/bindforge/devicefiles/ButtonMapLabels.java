package elite.intel.bindforge.devicefiles;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The labels a generated {@code .buttonMap} carries: {@code Button 1}...{@code Button N}, sized to what the
 * controller reports, and {@code X Axis} through {@code V Axis}.
 * <p>
 * <strong>Every label names its own token</strong>, so none is ever false - the point is that the capture dialog
 * and the game show a word rather than {@code Joy_14}. The user renames them in the Device Editor.
 * <p>
 * <strong>A controller with any axis gets all eight axis labels</strong> (Alan, 2026-10-06, after review). SDL
 * numbers a controller's axes without gaps, while the game names them by which axis they are: a stick reporting
 * X, Y and RZ is axes 0, 1 and 2 to SDL, and nothing in the report says which is which. Labelling by position
 * would label Z and leave the twist raw. A label for an axis the device lacks is never shown, so all eight cost a
 * few unused lines and miss nothing. The game names no axis past V.
 * <p>
 * <strong>No hat labels.</strong> {@code DeviceService} reads no hats, and in Elite Dangerous a hat mostly
 * reports as buttons anyway, which get {@code Button N} like any other.
 * <p>
 * The labels are English and fixed: they are file content the game displays, not Elite-Intel's interface text.
 */
public final class ButtonMapLabels {

    /**
     * Every joystick axis token the game has. Kept here rather than in {@code ButtonInputMapper}, whose list stops
     * at RZ: that class is shared with V1.1, and this list is only ever used to name labels.
     */
    private static final String[][] AXES = {
            {"Joy_XAxis", "X Axis"}, {"Joy_YAxis", "Y Axis"}, {"Joy_ZAxis", "Z Axis"},
            {"Joy_RXAxis", "RX Axis"}, {"Joy_RYAxis", "RY Axis"}, {"Joy_RZAxis", "RZ Axis"},
            {"Joy_UAxis", "U Axis"}, {"Joy_VAxis", "V Axis"}
    };

    private ButtonMapLabels() {
    }

    /**
     * @return label by token, buttons first and then axes, as Frontier's own maps are laid out; every axis token
     *         when the controller reports any axis; empty for a controller reporting neither
     */
    public static Map<String, String> generate(int buttonCount, int axisCount) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int button = 1; button <= buttonCount; button++) {
            labels.put("Joy_" + button, "Button " + button);
        }
        if (axisCount > 0) {
            for (String[] axis : AXES) labels.put(axis[0], axis[1]);
        }
        return labels;
    }
}
