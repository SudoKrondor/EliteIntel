package elite.intel.bindforge.devicefiles;

import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The labels a generated {@code .buttonMap} carries: {@code Button 1}...{@code Button N}, sized to what the
 * controller reports, then its axes ({@code X Axis}), then its hats ({@code Hat 1 Up}).
 * <p>
 * <strong>Every label names its own token</strong>, so none is ever false - the point is that the capture dialog
 * and the game show a word rather than {@code Joy_14}. The user renames them in the Device Editor.
 * <p>
 * <strong>Only the axes the controller has</strong> (Alan, 2026-10-10, A8b), read from its HID description: an X,
 * Y and RZ stick gets those three, so an unplugged stick's saved labels name the axes it really has. <strong>When
 * the description could not be read, or counts other than SDL does, a controller with any axis gets all
 * eight</strong> (Alan, 2026-10-06; the count after review, 2026-10-10) - a missing label leaves a raw token, an
 * unused one is never shown. SDL
 * numbers axes without gaps, so its count cannot say which they are, and a label for an axis the device lacks is
 * never shown. An axis the game cannot see gets no label - no token could name it.
 * <p>
 * <strong>Four labels per hat</strong> (Alan, 2026-10-10, A8b), in the spec's order: up, right, down, left. A hat
 * the firmware reports as buttons is buttons, and gets {@code Button N}.
 * <p>
 * The labels are English and fixed: they are file content the game displays, not Elite-Intel's interface text.
 */
public final class ButtonMapLabels {

    /** A hat's four directions, in the spec's order - also the order of SDL's hat bits, 1, 2, 4 and 8. */
    public static final List<String> HAT_DIRECTIONS = List.of("Up", "Right", "Down", "Left");

    private ButtonMapLabels() {
    }

    /**
     * @param axisCount how many axes SDL reports - checked against the description, and all eight axes are labelled
     *                  when the two disagree
     * @return label by token, buttons first, then axes, then hats; empty for a controller reporting none of them
     */
    public static Map<String, String> generate(int buttonCount, int axisCount, DeviceDescription description) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int button = 1; button <= buttonCount; button++) {
            labels.put(buttonToken(button), buttonLabel(button));
        }
        for (GameAxis axis : axesToLabel(axisCount, description)) {
            labels.put(axis.token(), axisLabel(axis));
        }
        for (int hat = 1; hat <= description.hatCount(); hat++) {
            for (String direction : HAT_DIRECTIONS) {
                labels.put(hatToken(hat, direction), hatLabel(hat, direction));
            }
        }
        return labels;
    }

    /**
     * The axes a controller is labelled for: those its description names when it agrees with SDL's count, and all
     * eight when it does not - the rule the Device Editor's AXES tab follows too (Alan, 2026-10-10, A9).
     */
    public static List<GameAxis> axesToLabel(int axisCount, DeviceDescription description) {
        if (description.agreesWith(axisCount)) return description.axes();
        return axisCount > 0 ? Arrays.asList(GameAxis.values()) : List.of();
    }

    /** {@code Joy_14} - 1-based, as the game numbers buttons. */
    public static String buttonToken(int button) {
        return "Joy_" + button;
    }

    public static String buttonLabel(int button) {
        return "Button " + button;
    }

    public static String axisLabel(GameAxis axis) {
        return axis.name() + " Axis";
    }

    /** {@code Joy_POV1Up} - hats 1-based, as the game numbers them. */
    public static String hatToken(int hat, String direction) {
        return "Joy_POV" + hat + direction;
    }

    public static String hatLabel(int hat, String direction) {
        return "Hat " + hat + " " + direction;
    }
}
