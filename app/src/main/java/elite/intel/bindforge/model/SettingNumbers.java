package elite.intel.bindforge.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The settings that are a number chosen freely: sensitivities, deadzones and power curves.
 * <p>
 * <strong>Two sets of limits, on purpose.</strong> {@link #judge} applies what Frontier's
 * {@code ControlSchemes/Help.txt} allows: sensitivity is <em>any number greater than 0.0</em> and a deadzone
 * <em>any number between 0.0 and 1.0</em>; the rule for sensitivity is applied to the rest of the family, whose
 * measured values are all above zero. {@link #range} is narrower: where the game's own sliders stop, read from two
 * files saved with every slider at its minimum and at its maximum (2026-11-04). Players' files go past those stops, so
 * a range places a slider and refuses nothing.
 */
public final class SettingNumbers {

    private static final Range SENSITIVITY = new Range(0.1f, 10f, 0.01f);
    private static final Range FREE_CAM_SENSITIVITY = new Range(0.1f, 20f, 0.1f);
    private static final Range FSS_SENSITIVITY = new Range(0.1f, 40f, 0.1f);
    private static final Range THIRD_PERSON_SENSITIVITY = new Range(0.1f, 200f, 0.1f);
    private static final Range AXIS_SENSITIVITY = new Range(0.25f, 4f, 0.01f);
    private static final Range YAW_TO_ROLL = new Range(0.1f, 0.9f, 0.01f);
    private static final Range DEADZONE = new Range(0f, 0.2f, 0.001f);
    private static final Range LINEARITY = new Range(1f, 3f, 0.01f);
    private static final Range DECAY_RATE = new Range(1f, 10f, 0.01f);

    /**
     * WHY {@code HeadlookMotionSensitivity} is missing: both measuring passes left it at 1, as does nearly every file
     * in the player corpus, so no stop has ever been read for it. It stays a typed number.
     */
    private static final Map<String, Range> RANGES = Map.ofEntries(
            Map.entry("BuggyTurretMouseDeadzone", DEADZONE),
            Map.entry("BuggyTurretMouseLinearity", LINEARITY),
            Map.entry("BuggyTurretMouseSensitivity", SENSITIVITY),
            Map.entry("FSSMouseDeadzone", DEADZONE),
            Map.entry("FSSMouseLinearity", LINEARITY),
            Map.entry("FSSMouseSensitivity", FSS_SENSITIVITY),
            Map.entry("FSSTuningSensitivity", SENSITIVITY),
            Map.entry("FreeCamMouseSensitivity", FREE_CAM_SENSITIVITY),
            Map.entry("HeadlookSensitivity", AXIS_SENSITIVITY),
            Map.entry("HumanoidPitchSensitivity", AXIS_SENSITIVITY),
            Map.entry("HumanoidRotateSensitivity", AXIS_SENSITIVITY),
            Map.entry("MouseDeadzone", DEADZONE),
            Map.entry("MouseDecayRate", DECAY_RATE),
            Map.entry("MouseHeadlookSensitivity", SENSITIVITY),
            Map.entry("MouseHumanoidSensitivity", SENSITIVITY),
            Map.entry("MouseLinearity", LINEARITY),
            Map.entry("MouseSensitivity", SENSITIVITY),
            Map.entry("MultiCrewThirdPersonMouseSensitivity", THIRD_PERSON_SENSITIVITY),
            Map.entry("PlacementCamMouseSensitivity", FREE_CAM_SENSITIVITY),
            Map.entry("SAAThirdPersonMouseSensitivity", THIRD_PERSON_SENSITIVITY),
            Map.entry("YawToRollSensitivity", YAW_TO_ROLL));

    public static final Set<String> SETTINGS = Set.of(
            "BuggyTurretMouseDeadzone", "BuggyTurretMouseLinearity", "BuggyTurretMouseSensitivity",
            "FSSMouseDeadzone", "FSSMouseLinearity", "FSSMouseSensitivity", "FSSTuningSensitivity",
            "FreeCamMouseSensitivity", "HeadlookMotionSensitivity", "HeadlookSensitivity",
            "HumanoidPitchSensitivity", "HumanoidRotateSensitivity",
            "MouseDeadzone", "MouseDecayRate", "MouseHeadlookSensitivity", "MouseHumanoidSensitivity",
            "MouseLinearity", "MouseSensitivity", "MultiCrewThirdPersonMouseSensitivity",
            "PlacementCamMouseSensitivity", "SAAThirdPersonMouseSensitivity", "YawToRollSensitivity");

    public enum Refusal {NOT_A_NUMBER, MUST_BE_ABOVE_ZERO, MUST_BE_ZERO_TO_ONE}

    /**
     * What was typed, judged: {@code value} when it may be written, else {@code refusal}.
     */
    public record Verdict(Float value, Refusal refusal) {
        public boolean valid() {
            return refusal == null;
        }
    }

    /**
     * Where the game's slider for a setting stops, and the step ours moves in. A slider counts whole numbers, so
     * {@link #toSlider} and {@link #fromSlider} carry a value on a scale of thousandths.
     */
    public record Range(float min, float max, float step) {
        private static final int SLIDER_SCALE = 1000;

        public int sliderMin() {
            return toSlider(min);
        }

        public int sliderMax() {
            return toSlider(max);
        }

        public int sliderStep() {
            return toSlider(step);
        }

        public int toSlider(float value) {
            return Math.round(value * SLIDER_SCALE);
        }

        public float fromSlider(int position) {
            return position / (float) SLIDER_SCALE;
        }

        public boolean contains(float value) {
            return value >= min && value <= max;
        }
    }

    private SettingNumbers() {
    }

    /**
     * The game's slider range for {@code setting}; empty when none has been measured.
     */
    public static Optional<Range> range(String setting) {
        return Optional.ofNullable(RANGES.get(setting));
    }

    /**
     * Takes either decimal mark, since half the app's languages type a comma.
     */
    public static Verdict judge(String setting, String typed) {
        float number;
        try {
            number = Float.parseFloat(typed.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            return new Verdict(null, Refusal.NOT_A_NUMBER);
        }
        if (Float.isNaN(number) || Float.isInfinite(number)) return new Verdict(null, Refusal.NOT_A_NUMBER);
        if (setting.endsWith("Deadzone")) {
            return number >= 0f && number <= 1f
                    ? new Verdict(number, null) : new Verdict(null, Refusal.MUST_BE_ZERO_TO_ONE);
        }
        return number > 0f ? new Verdict(number, null) : new Verdict(null, Refusal.MUST_BE_ABOVE_ZERO);
    }

    /**
     * A number as the game writes a float: eight decimals.
     */
    public static String written(float value) {
        return String.format(Locale.ROOT, "%.8f", (double) value);
    }

    /**
     * What the file holds, without the game's trailing zeros or a float's rounding noise (0.40000001 is the float
     * nearest 0.4, and reads as 0.4); text that is not a number is shown as it is.
     */
    public static String shown(String value) {
        try {
            float held = new BigDecimal(value.strip()).floatValue();
            BigDecimal number = new BigDecimal(Float.toString(held)).stripTrailingZeros();
            return number.scale() <= 0 ? number.setScale(1).toPlainString() : number.toPlainString();
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
