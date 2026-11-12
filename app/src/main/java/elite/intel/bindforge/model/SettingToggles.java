package elite.intel.bindforge.model;

import java.util.Set;

/**
 * The settings that are a yes or a no, which the Bind Editor's Toggles tab lists (bind-editor.md, <em>Toggles</em>).
 * <p>
 * The booleans list of {@code settings-choice-fields.md}, but {@code Hold}, {@code Inverted} and {@code ToggleOn}: those
 * are parts of a binding, and belong to the bind dialogs.
 * <p>
 * <strong>Only {@code 1} is on</strong> (Alan, 2026-11-11, E10 Q5). The game writes {@code 0} or {@code 1}; anything
 * else in the file reads as off, and nothing is written until the user turns it on or off, when {@code 1} or {@code 0}
 * replaces it.
 */
public final class SettingToggles {

    public static final Set<String> SETTINGS = Set.of(
            "DeployHardpointsOnFire", "DriveAssistDefault", "EnableAimAssistOnFoot", "EnableCameraLockOn",
            "EnableMenuGroups", "EnableMenuGroupsOnFoot", "EnableMenuGroupsSRV", "EnableRumbleTrigger",
            "FSSMouseXDecay", "FSSMouseYDecay", "FocusOnTextEntryField", "FreeCamMouseXDecay", "FreeCamMouseYDecay",
            "HeadlookDefault", "HeadlookResetOnToggle", "HeadlookSmoothing", "HumanoidItemWheel_AcceptMouseInput",
            "MotionHeadlook", "MouseBuggyRollingXDecay", "MouseBuggySteeringXDecay", "MouseBuggyYDecay", "MouseDecay",
            "MouseGUI", "MouseHeadingUpInsteadOfRoll", "MouseHeadlook", "MouseHeadlookInvert", "MouseRelativeMode",
            "MouseTurretXDecay", "MouseTurretYDecay", "MouseWheelThrottle", "MouseWheelThrottleInverted",
            "MouseXDecay", "MouseXSign", "MouseYDecay", "MouseYSign", "MultiCrewThirdPersonMouseXDecay",
            "MultiCrewThirdPersonMouseYDecay", "PlacementCamMouseXDecay", "PlacementCamMouseYDecay",
            "SAAThirdPersonMouseXDecay", "SAAThirdPersonMouseYDecay", "yawRotateHeadlook");

    private SettingToggles() {
    }

    /**
     * Whether a value as the file holds it reads as on.
     */
    public static boolean on(String value) {
        return "1".equals(value);
    }

    /**
     * What the game writes for on and off.
     */
    public static String written(boolean on) {
        return on ? "1" : "0";
    }
}
