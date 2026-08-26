package elite.intel.ai.hands;

import elite.intel.util.StringUtls;

import java.util.HashMap;
import java.util.Map;

/**
 * Curated descriptions for known-dangerous binding pairs.
 * Also owns the context-group logic used by conflict detection: which actions live in mutually
 * exclusive input contexts ({@link #contextOf}) and which live in modal sub-state overlays
 * ({@link #isSubStateModeAction}). Two actions can only conflict when they share both a context and
 * an exact chord.
 */
public class BindingConflictRules {

    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        // NOTE: UI_* navigation keys are NOT listed here. The UI panel is its own input context
        // (the game disables ship controls while a panel is open), so UI_* never co-fires with a
        // ship action - see contextOf(). Two UI_* actions on one chord still conflict (same context).

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
        if (isMapVersusUiNavigation(a, b)) {
            String map = isMapCameraAction(a) ? a : b;
            String ui = isMapCameraAction(a) ? b : a;
            return StringUtls.humanizeBindingName(map) + " and " + StringUtls.humanizeBindingName(ui)
                    + " share a key - inside the galaxy/system map both are live at once, so map movement"
                    + " and panel navigation will fight each other";
        }
        return StringUtls.humanizeBindingName(a) + " and " + StringUtls.humanizeBindingName(b) + " share a key and may interfere";
    }

    /**
     * Returns true when two actions sharing a key is safe and should not be flagged.
     * <p>
     * Unsafe first: {@link #isMapVersusUiNavigation} - the galaxy/system map is the one overlay where
     * two families are live <em>simultaneously</em>, so it is checked before the mutual-exclusion
     * rules below (which would otherwise clear it twice over).
     * <p>
     * Safe cases:
     * - Different input contexts (ship / buggy / humanoid / UI / construction) - mutually exclusive,
     * only one is active at a time. The game disables the others' controls while one is active, so a
     * shared key cannot fire two of them. In particular a UI_* or construction-panel action never
     * collides with a ship action (e.g. {@code CycleNextSubsystem} vs {@code UI_Right}).
     * - Either action belongs to a sub-state overlay (camera, FSS, Galnet, radial wheels) - these
     * modes are only active inside a specific overlay and cannot fire alongside regular actions.
     */
    public static boolean isSafeOverlap(String a, String b) {
        if (isMapVersusUiNavigation(a, b)) return false;
        if (isSubStateModeAction(a) || isSubStateModeAction(b)) return true;
        return !contextOf(a).equals(contextOf(b));
    }

    /**
     * True when a conflict is <b>blocking</b>: it stops EliteIntel from driving the game at all, not merely
     * "may interfere". Today that is exactly the map-camera-versus-UI-navigation overlap.
     * <p>
     * WHY this one is in a class of its own: route plotting - the single most-used function - walks the galaxy
     * map to its search field with {@code UI_Left}/{@code UI_Right}/{@code UI_Select} taps. A commander doing
     * that by hand recovers from the collision without noticing, because they click the field with the mouse.
     * EliteIntel has no mouse; the keyboard walk is the only way in. So when the same chord also pans the map,
     * focus never reaches the search field, the system name is typed into nothing, and no route is plotted -
     * silently, with every keystroke reporting success. Frontier's own defaults land on this, which is why it
     * is announced on every start rather than once (see {@code KeyBindCheck}).
     * <p>
     * The remedy is separation, not a specific layout: W/A/S/D for the map and the arrow keys for the
     * interface is fine, and so is the reverse. The same keys for both is not.
     */
    public static boolean isBlocking(String a, String b) {
        return isMapVersusUiNavigation(a, b);
    }

    /**
     * True when one action drives the galaxy/system map camera and the other is UI panel navigation.
     * <p>
     * This is the one case where the context model's "only one context is active" assumption breaks.
     * Everywhere else the map camera behaves like a sub-state overlay and UI_* like its own context,
     * so the two would be cleared twice over - but while the map is <em>open</em> both families are
     * live at the same time: the {@code Cam*} keys pan/zoom the holographic map while {@code UI_*}
     * moves the cursor through the map's panels and tabs. A shared chord fires both, and the map
     * stops responding to movement correctly (reported in the field with W/A/S/D bound to
     * {@code CamTranslate*} and {@code UI_Up}/{@code UI_Down}/{@code UI_Left}/{@code UI_Right}).
     */
    private static boolean isMapVersusUiNavigation(String a, String b) {
        return (isMapCameraAction(a) && isUiNavigationAction(b))
                || (isMapCameraAction(b) && isUiNavigationAction(a));
    }

    /**
     * The four action families Elite groups under the galaxy/system map sections: {@code CamTranslate*}
     * (pan), {@code CamPitch*} and {@code CamYaw*} (orbit), {@code CamZoom*}, plus {@code GalaxyMapHome}
     * (a map-internal control, not the map-open toggle).
     * <p>
     * WHY: listed prefix by prefix rather than matching a bare {@code Cam} prefix. The action set belongs
     * to Frontier, not to {@link Bindings}, so a future game update could ship a {@code Cam*} action in
     * some unrelated context and it would silently start colliding with every {@code UI_*} binding. A new
     * family has to be opted in here deliberately.
     * <p>
     * The other camera families all carry their own prefix ({@code FreeCam*}, {@code MoveFreeCam*},
     * {@code PitchCamera*}, {@code MovePlacementCam*}, {@code StoreCam*}, {@code VanityCamera*}) and are
     * excluded: none of them can be open at the same time as a UI panel.
     */
    private static boolean isMapCameraAction(String action) {
        return action.startsWith("CamTranslate")
                || action.startsWith("CamPitch")
                || action.startsWith("CamYaw")
                || action.startsWith("CamZoom")
                || action.equals("GalaxyMapHome");
    }

    /**
     * UI panel navigation ({@code UI_Up}, {@code UI_Left}, {@code UI_Select}, {@code UI_Back}, …).
     * The whole family stays live while the map is open, not just the four direction keys.
     */
    private static boolean isUiNavigationAction(String action) {
        return action.startsWith("UI_");
    }

    /**
     * Sorted, order-independent key for a pair of action names.
     */
    public static String makeKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private static final String BUGGY_SUFFIX = "_Buggy";

    /**
     * The ship action that is the same logical control as the given SRV ({@code _Buggy}) action, or
     * {@code null} when the action is not an SRV variant.
     * <p>
     * Ship and SRV are mutually exclusive vehicles with effectively the same controls. Elite names
     * every SRV action {@code <ShipAction>_Buggy}, so stripping the suffix yields its ship twin. A
     * control and its twin sharing a key never conflict ({@link #isSafeOverlap}); binding them to the
     * <em>same</em> key is the recommended-but-not-required setup, which the editor nudges toward.
     */
    public static String shipTwinOf(String action) {
        if (action == null || !action.endsWith(BUGGY_SUFFIX)) {
            return null;
        }
        return action.substring(0, action.length() - BUGGY_SUFFIX.length());
    }

    /**
     * The mutually exclusive input context an action belongs to. Only one is ever active, so two
     * actions in different contexts can share a key without ever co-firing:
     * <ul>
     *   <li>{@code ui} - panel/menu navigation (UI_* keys); ship controls are disabled while a panel is open;</li>
     *   <li>{@code construction} - the colonisation/construction panel, a separate UI panel;</li>
     *   <li>{@code buggy} - SRV (_Buggy) controls;</li>
     *   <li>{@code humanoid} - on-foot controls;</li>
     *   <li>{@code ship} - everything else (cockpit flight).</li>
     * </ul>
     */
    private static String contextOf(String action) {
        if (action.startsWith("UI_")) return "ui";
        if (action.contains("Construction")) return "construction";
        if (action.endsWith("_Buggy")) return "buggy";
        if (action.contains("Humanoid")) return "humanoid";
        return "ship";
    }

    /**
     * Sub-state overlays active only inside a specific mode (camera, FSS scanner, Galnet, radial
     * wheels). Key sharing between these and main-state actions is safe.
     */
    private static boolean isSubStateModeAction(String action) {
        // WHY: "Cam" is matched as a substring (not a prefix) on purpose - it covers every camera
        // action family at once: FreeCam*, MoveFreeCam*, FixCamera*, PhotoCamera*, QuitCamera, and
        // all the CamPitch/CamRoll/CamYaw/CamZoom/CamTranslate axes. No non-camera ED action name
        // contains "Cam".
        return action.contains("Cam")
                // Radial wheels (HumanoidItemWheel*, HumanoidEmoteWheel*, HumanoidUtilityWheel*) are
                // modal UI components: while a wheel is shown the game blocks every other control, so
                // they cannot co-fire with any other action.
                || action.contains("Wheel")
                || action.startsWith("Vanity")
                || action.startsWith("MovePlacement") || action.startsWith("Placement")
                || action.startsWith("GalnetAudio")
                || action.startsWith("MultiCrew") || action.startsWith("Store")
                || action.startsWith("ExplorationFSS") || action.startsWith("ExplorationSAA");
    }
}
