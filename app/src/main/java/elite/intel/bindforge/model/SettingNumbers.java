package elite.intel.bindforge.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * The settings that are a number chosen freely: sensitivities, deadzones and power curves.
 * <p>
 * <strong>No upper limits, on purpose.</strong> The game's slider limits are written down nowhere: Frontier's
 * {@code ControlSchemes/Help.txt} says only that sensitivity is <em>any number greater than 0.0</em> and a deadzone
 * <em>any number between 0.0 and 1.0</em>, and players' files go past what the sliders offer. So a value is checked
 * against those two rules and nothing else; the rule for sensitivity is applied to the rest of the family, whose
 * measured values are all above zero.
 */
public final class SettingNumbers {

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

    private SettingNumbers() {
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
     * What the file holds, without the game's trailing zeros; text that is not a number is shown as it is.
     */
    public static String shown(String value) {
        try {
            BigDecimal number = new BigDecimal(value.strip()).stripTrailingZeros();
            return number.scale() <= 0 ? number.setScale(1).toPlainString() : number.toPlainString();
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
