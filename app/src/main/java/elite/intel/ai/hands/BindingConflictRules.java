package elite.intel.ai.hands;

import elite.intel.util.StringUtls;

import java.util.HashMap;
import java.util.Map;

/**
 * Curated descriptions for known-dangerous binding pairs.
 * Also owns the context-group logic used by conflict detection.
 */
public class BindingConflictRules {

    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        // UI navigation keys firing ship actions accidentally
        put("UI_Back", "ToggleFlightAssist", "Closing a panel will toggle flight assist");
        put("UI_Back", "DeployHardpointToggle", "Closing a panel will deploy or retract hardpoints");
        put("UI_Back", "ToggleCargoScoop", "Closing a panel will toggle the cargo scoop");
        put("UI_Back", "LandingGearToggle", "Closing a panel will toggle landing gear");
        put("UI_Back", "ShipSpotLightToggle", "Closing a panel will toggle ship lights");
        put("UI_Back", "NightVisionToggle", "Closing a panel will toggle night vision");
        put("UI_Back", "Supercruise", "Closing a panel will engage or exit supercruise");
        put("UI_Back", "Hyperspace", "Closing a panel will initiate a hyperspace jump");
        put("UI_Back", "RecallDismissShip", "Closing a panel will recall or dismiss the ship");

        put("UI_Toggle", "ToggleFlightAssist", "Toggling the UI will toggle flight assist");
        put("UI_Toggle", "DeployHardpointToggle", "Toggling the UI will deploy or retract hardpoints");
        put("UI_Toggle", "ToggleCargoScoop", "Toggling the UI will toggle the cargo scoop");
        put("UI_Toggle", "NightVisionToggle", "Toggling the UI will activate night vision");
        put("UI_Toggle", "Supercruise", "Toggling the UI will engage or exit supercruise");

        // Dangerous hardware action pairs
        put("DeployHardpointToggle", "LandingGearToggle", "Deploying hardpoints will also toggle landing gear");
        put("DeployHardpointToggle", "ToggleCargoScoop", "Deploying hardpoints will also toggle the cargo scoop");
        put("LandingGearToggle", "ToggleCargoScoop", "Toggling landing gear will also toggle the cargo scoop");
        put("Supercruise", "Hyperspace", "Supercruise and hyperspace share a key - jump type will depend on target");
    }

    private static void put(String a, String b, String description) {
        DESCRIPTIONS.put(makeKey(a, b), description);
    }

    public static String describe(String a, String b) {
        String d = DESCRIPTIONS.get(makeKey(a, b));
        if (d != null) return d;
        return StringUtls.humanizeBindingName(a) + " and " + StringUtls.humanizeBindingName(b) + " share a key and may interfere";
    }

    /**
     * Returns true when two actions sharing a key is safe and should not be flagged.
     * <p>
     * Safe cases:
     * - Different vehicle states (ship / buggy / humanoid) - mutually exclusive, player
     * can only be in one at a time. The AiActionsMap also filters on state, so the
     * game itself prevents simultaneous activation.
     * - Either action belongs to a sub-state overlay (camera, FSS, Galnet) - these
     * modes are only active inside a specific overlay and cannot fire alongside
     * regular ship actions.
     */
    public static boolean isSafeOverlap(String a, String b) {
        if (isSubStateModeAction(a) || isSubStateModeAction(b)) return true;
        return !vehicleStateOf(a).equals(vehicleStateOf(b));
    }

    /**
     * Sorted, order-independent key for a pair of action names.
     */
    public static String makeKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /**
     * Ship / buggy (SRV) / humanoid (on-foot) - the three mutually exclusive player states.
     */
    private static String vehicleStateOf(String action) {
        if (action.endsWith("_Buggy")) return "buggy";
        if (action.contains("Humanoid")) return "humanoid";
        return "ship";
    }

    /**
     * Sub-state overlays active only inside a specific mode (camera, FSS scanner,
     * Galnet). Key sharing between these and main-state actions is safe.
     */
    private static boolean isSubStateModeAction(String action) {
        // WHY: "Cam" is matched as a substring (not a prefix) on purpose - it covers every camera
        // action family at once: FreeCam*, MoveFreeCam*, FixCamera*, PhotoCamera*, QuitCamera, and
        // all the CamPitch/CamRoll/CamYaw/CamZoom/CamTranslate axes. No non-camera ED action name
        // contains "Cam".
        return action.contains("Cam")
                || action.startsWith("Vanity")
                || action.startsWith("MovePlacement") || action.startsWith("Placement")
                || action.startsWith("GalnetAudio")
                || action.startsWith("MultiCrew") || action.startsWith("Store")
                || action.startsWith("ExplorationFSS") || action.startsWith("ExplorationSAA");
    }
}
