package elite.intel.ai.hands;

import elite.intel.util.StringUtls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Curated descriptions for known-dangerous binding pairs.
 * Also owns the context-group logic used by conflict detection: which actions live in mutually
 * exclusive input contexts ({@link #contextOf}) and which live in modal sub-state overlays
 * ({@link #isSubStateModeAction}). Two actions can only conflict when they share both a context and
 * an exact chord.
 */
public class BindingConflictRules {

    private static final String UI_SELECT = "UI_Select";

    /**
     * The quick comms panel in all three vehicle contexts. Elite gives the ship, SRV and on-foot
     * variants separate action names, and the chord clashes the same way in each.
     */
    private static final List<String> QUICK_COMMS_ACTIONS = List.of(
            "QuickCommsPanel", "QuickCommsPanel_Buggy", "QuickCommsPanel_Humanoid");

    /**
     * The controls that change what the interface is showing: the four panel-focus keys and the two
     * map-open toggles, in each vehicle context Elite names them for.
     * <p>
     * The third break in the context model, for the same reason as {@link #isMapVersusUiNavigation}
     * and {@link #isSelectVersusQuickComms}: these keys stay live <em>while a panel or map is open</em>
     * - that is how a commander switches from the right panel to the left one, or drops into the galaxy
     * map without closing what they were looking at first. So "UI and vehicle controls are never live
     * together" does not hold for this family, and {@link #contextOf} would clear an SRV map key sharing
     * a chord with {@code UI_Down} as two different contexts.
     * <p>
     * Listed by name rather than matched on a prefix, for the reason {@link #isMapCameraAction} is: the
     * action set is Frontier's. A control they add later has to be opted in here by someone who has
     * decided it belongs.
     * <p>
     * The quick comms panel is deliberately absent even though it behaves the same way: it has its own
     * rule ({@link #isSelectVersusQuickComms}), which reports it against {@code UI_Select} alone and
     * leaves it an ordinary overlap against the direction keys.
     */
    private static final Set<String> INTERFACE_SWITCH_ACTIONS = Set.of(
            "FocusLeftPanel", "FocusLeftPanel_Buggy",
            "FocusCommsPanel", "FocusCommsPanel_Buggy", "FocusCommsPanel_Humanoid",
            "FocusRadarPanel", "FocusRadarPanel_Buggy",
            "FocusRightPanel", "FocusRightPanel_Buggy",
            "GalaxyMapOpen", "GalaxyMapOpen_Buggy", "GalaxyMapOpen_Humanoid",
            "SystemMapOpen", "SystemMapOpen_Buggy", "SystemMapOpen_Humanoid");

    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        // NOTE: UI_* navigation keys are otherwise NOT listed here. The UI panel is its own input
        // context (the game disables ship controls while a panel is open), so UI_* never co-fires with
        // a ship action - see contextOf(). Two UI_* actions on one chord still conflict (same context).
        // The quick comms pairs below are the exception, and say why; the panel-focus and map-open keys
        // are the other one, described generically in describe() rather than pair by pair here.

        // Dangerous hardware action pairs
        put("DeployHardpointToggle", "LandingGearToggle", "Deploying hardpoints will also toggle landing gear");
        put("DeployHardpointToggle", "ToggleCargoScoop", "Deploying hardpoints will also toggle the cargo scoop");
        put("LandingGearToggle", "ToggleCargoScoop", "Toggling landing gear will also toggle the cargo scoop");
        put("Supercruise", "Hyperspace", "Supercruise and hyperspace share a key - jump type will depend on target");

        // The interface Select key opening the chat box as well - see isSelectVersusQuickComms().
        for (String comms : QUICK_COMMS_ACTIONS) {
            put(UI_SELECT, comms, "Select and the quick comms panel share a key - every interface selection"
                    + " EliteIntel makes also opens the chat text box, which then swallows the keys that follow"
                    + " and can broadcast them");
        }
    }

    private static void put(String a, String b, String description) {
        DESCRIPTIONS.put(makeKey(a, b), description);
    }

    /**
     * True when {@link #describe} has specific wording for this pair rather than the generic
     * "share a key and may interfere". Those are the pairs with a consequence worth stating in
     * full - deploying hardpoints also dropping the landing gear - so the startup report gives
     * them a line of their own while the generic overlaps collapse into a list.
     */
    public static boolean hasCuratedDescription(String a, String b) {
        return DESCRIPTIONS.containsKey(makeKey(a, b));
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
        if (isInterfaceSwitchVersusUiNavigation(a, b)) {
            String panel = INTERFACE_SWITCH_ACTIONS.contains(a) ? a : b;
            String ui = INTERFACE_SWITCH_ACTIONS.contains(a) ? b : a;
            return StringUtls.humanizeBindingName(panel) + " and " + StringUtls.humanizeBindingName(ui)
                    + " share a key - the panel and map keys stay live while a panel is open, so every step"
                    + " EliteIntel takes through the interface also fires "
                    + StringUtls.humanizeBindingName(panel) + " and the walk never reaches what it was sent for";
        }
        return StringUtls.humanizeBindingName(a) + " and " + StringUtls.humanizeBindingName(b) + " share a key and may interfere";
    }

    /**
     * Returns true when two actions sharing a key is safe and should not be flagged.
     * <p>
     * Unsafe first: {@link #isMapVersusUiNavigation}, {@link #isSelectVersusQuickComms} and
     * {@link #isInterfaceSwitchVersusUiNavigation} - the three cases where families the context model
     * treats as mutually exclusive are in fact live <em>simultaneously</em>, so they are checked before
     * the mutual-exclusion rules below (which would otherwise clear them twice over).
     * <p>
     * Safe cases:
     * - Different input contexts (ship / buggy / humanoid / UI / construction) - mutually exclusive,
     * only one is active at a time. The game disables the others' controls while one is active, so a
     * shared key cannot fire two of them. In particular a UI_* or construction-panel action never
     * collides with a ship action (e.g. {@code CycleNextSubsystem} vs {@code UI_Right}), and a control
     * the commander thinks of as one thing but Elite binds per vehicle - the cargo scoop, the fire
     * groups, the triggers, the panels, the maps, the lamps, night vision - may sit on one key in all
     * of them. Commanders lay it out that way on purpose, and only one vehicle is ever occupied.
     * - Either action belongs to a sub-state overlay (camera, FSS, Galnet, radial wheels) - these
     * modes are only active inside a specific overlay and cannot fire alongside regular actions.
     */
    public static boolean isSafeOverlap(String a, String b) {
        if (isMapVersusUiNavigation(a, b)) return false;
        if (isSelectVersusQuickComms(a, b)) return false;
        if (isInterfaceSwitchVersusUiNavigation(a, b)) return false;
        if (isSubStateModeAction(a) || isSubStateModeAction(b)) return true;
        return !contextOf(a).equals(contextOf(b));
    }

    /**
     * True when a conflict is <b>blocking</b>: it stops EliteIntel from driving the game at all, not merely
     * "may interfere". Today that is the map-camera-versus-UI-navigation overlap
     * ({@link #isMapVersusUiNavigation}), Select sharing a key with the quick comms panel
     * ({@link #isSelectVersusQuickComms}), and a panel-focus or map-open key sharing a chord with UI
     * navigation ({@link #isInterfaceSwitchVersusUiNavigation}).
     * <p>
     * WHY the map overlap is in a class of its own: route plotting - the single most-used function - walks the galaxy
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
        return isMapVersusUiNavigation(a, b)
                || isSelectVersusQuickComms(a, b)
                || isInterfaceSwitchVersusUiNavigation(a, b);
    }

    /**
     * True when one action is {@code UI_Select} and the other is the quick comms panel (in any of its
     * three vehicle variants).
     * <p>
     * The second break in the context model, for the same reason as {@link #isMapVersusUiNavigation}:
     * the comms panel is reachable <em>while</em> an interface panel is open, so "UI and ship controls
     * are never live together" does not hold for this pair and {@link #contextOf} would clear it.
     * <p>
     * WHY it is blocking rather than an overlap that "may interfere": Select is how EliteIntel commits
     * every choice it makes in the interface - the galaxy map search field, the route, panel entries.
     * With the comms panel on the same chord, each of those taps also drops a focused chat text box on
     * screen, which takes the keyboard. The system name EliteIntel types next goes into chat instead of
     * the search field, so nothing is plotted - and what was typed can be sent to other commanders.
     * A commander doing the same thing by hand never sees it, because they click with the mouse.
     */
    private static boolean isSelectVersusQuickComms(String a, String b) {
        return (UI_SELECT.equals(a) && QUICK_COMMS_ACTIONS.contains(b))
                || (UI_SELECT.equals(b) && QUICK_COMMS_ACTIONS.contains(a));
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
     * True when one action changes what the interface is showing - a panel-focus key or a map-open
     * toggle, in any vehicle context ({@link #INTERFACE_SWITCH_ACTIONS}) - and the other is UI panel
     * navigation.
     * <p>
     * WHY it is blocking rather than an overlap that "may interfere": every panel EliteIntel opens it
     * then walks with {@code UI_*} taps - the role panel to recover an SRV, the right panel to a module,
     * the galaxy map to its search field. These keys are not disabled while that panel is open, so a
     * shared chord means each step of the walk also switches panel or throws the map up over it. The
     * walk then runs on blind against whatever is now on screen, every keystroke reporting success.
     * <p>
     * A commander doing it by hand never sees it: they are looking at the screen and simply stop when
     * the map appears. Reported from a support bundle of 2026-09-09, where {@code UI_Down} and
     * {@code GalaxyMapOpen_Buggy} were both on {@code Ctrl+S} - so every attempt to recover an SRV, and
     * every attempt to open the role panel, opened the galaxy map instead.
     * <p>
     * The remedy is separation, as with {@link #isMapVersusUiNavigation}: the interface keys and the
     * panel/map keys have to be different chords. Which layout they use is theirs to pick.
     */
    private static boolean isInterfaceSwitchVersusUiNavigation(String a, String b) {
        return (INTERFACE_SWITCH_ACTIONS.contains(a) && isUiNavigationAction(b))
                || (INTERFACE_SWITCH_ACTIONS.contains(b) && isUiNavigationAction(a));
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
     * Ship and SRV are mutually exclusive vehicles with effectively the same controls. Where an SRV
     * control has a ship counterpart of the same name, Elite spells it {@code <ShipAction>_Buggy}, so
     * stripping the suffix yields its ship twin (the SRV-only drive and turret controls are spelled
     * {@code Buggy*} instead and have no twin). A control and its twin sharing a key never conflict
     * ({@link #isSafeOverlap}); binding them to the
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
     *   <li>{@code ui} - panel/menu navigation (UI_* keys); ship controls are disabled while a panel is open,
     *       with the three exceptions checked ahead of this in {@link #isSafeOverlap};</li>
     *   <li>{@code construction} - the colonisation/construction panel, a separate UI panel;</li>
     *   <li>{@code buggy} - SRV controls;</li>
     *   <li>{@code humanoid} - on-foot controls;</li>
     *   <li>{@code ship} - everything else (cockpit flight).</li>
     * </ul>
     * The vehicle is read from the game's own controls screen ({@link BindingDisplayNames}), because
     * Elite's tag names do not say it reliably: the SRV fire and drive controls are spelled
     * {@code BuggyPrimaryFireButton}, {@code SteerLeftButton}, {@code VerticalThrustersButton} - no
     * {@code _Buggy} suffix - and reading them as ship actions reported the SRV trigger against the ship
     * trigger, the SRV fire groups against the ship fire groups, and SRV steering against yaw, on layouts
     * where sharing those keys is exactly the point. A tag the table has not met (or one under GENERAL,
     * which holds the interface and camera controls the checks above already classify) falls back to the
     * name, which is right for the {@code _Buggy} and {@code Humanoid} spellings.
     */
    private static String contextOf(String action) {
        if (action.startsWith("UI_")) return "ui";
        if (action.contains("Construction")) return "construction";
        return switch (BindingDisplayNames.lookup(action).section()) {
            case SRV -> "buggy";
            case ON_FOOT -> "humanoid";
            case SHIP -> "ship";
            case GENERAL, OTHER -> contextFromName(action);
        };
    }

    private static String contextFromName(String action) {
        if (action.contains("Buggy")) return "buggy";
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
                // "Galaxy Cam Select Current System": the one control in the map's own section whose
                // tag does not spell "Cam". It exists only while the map is open, like the rest.
                || action.equals("GalaxyMapHome")
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
