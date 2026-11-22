package elite.intel.bindforge.model;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Full Range / Forward Only choice some throttle axes carry in a bare setting beside them. The three fields do not
 * share a vocabulary, and a list may not be copied from its family (settings-choice-fields.md).
 * <p>
 * Full range is written as the empty value, which is what the game writes (bind-editor.md: {@code ""} = FULL RANGE). A
 * value neither list knows is a hand edit: it reads as {@link Reading#UNRECOGNISED} and stays in the file unless the
 * commander picks something else.
 */
public final class ThrottleRanges {

    public enum Reading {FULL_RANGE, FORWARD_ONLY, UNRECOGNISED}

    /**
     * @param setting     the bare setting's element
     * @param forwardOnly the token written for Forward Only
     */
    public record Range(String setting, String forwardOnly, Set<String> readsForward, Set<String> readsFull) {

        public Reading read(String value) {
            if (readsForward.contains(value)) return Reading.FORWARD_ONLY;
            return readsFull.contains(value) ? Reading.FULL_RANGE : Reading.UNRECOGNISED;
        }

        /**
         * The value that makes the choice; Forward Only for anything but Full Range.
         */
        public String valueFor(Reading reading) {
            return reading == Reading.FULL_RANGE ? FULL_RANGE_VALUE : forwardOnly;
        }
    }

    public static final String FULL_RANGE_VALUE = "";

    private static final Range SHIP = new Range("ThrottleRange", "Bindings_ThrottleForewardOnly",
            Set.of("Bindings_ThrottleForewardOnly"), Set.of("", "Bindings_ThrottleFullRange"));
    private static final Range SRV = new Range("BuggyThrottleRange", "Bindings_BuggyThrottleForewardOnly",
            Set.of("Bindings_BuggyThrottleForewardOnly"), Set.of(""));
    private static final Range FREE_CAM = new Range("ThrottleRangeFreeCam", "Bindings_ThrottleForewardOnlyFreeCam",
            Set.of("Bindings_ThrottleForewardOnlyFreeCam", "Bindings_ThrottleForewardOnly"),
            Set.of("", "Bindings_ThrottleFullRangeFreeCam"));

    private static final Map<String, Range> BY_AXIS = Map.of(
            "ThrottleAxis", SHIP,
            "DriveSpeedAxis", SRV,
            "MoveFreeCamY", FREE_CAM);

    private ThrottleRanges() {
    }

    /**
     * The range setting that goes with this axis; empty for an axis that has none.
     */
    public static Optional<Range> forAxis(String axisId) {
        return Optional.ofNullable(BY_AXIS.get(axisId));
    }
}
