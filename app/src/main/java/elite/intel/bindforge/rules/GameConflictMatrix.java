package elite.intel.bindforge.rules;

import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.model.BindingSection;

import java.util.HashSet;
import java.util.Set;

/**
 * Which of the game's control groups warn when they share a key: Frontier's own rebind check, as tested pair by pair
 * in game on 2026-07-30. The data is {@code docs/.../domain-knowledge/EliteDangerous-ConflictMatrix.md}, and
 * {@code GameConflictMatrixTest} holds this copy to that table cell by cell.
 * <p>
 * <strong>It only ever adds a conflict.</strong> A {@code no} here does not clear anything: {@link BindingConflictRules}
 * also reports pairs the game lets through, because of which panel has focus, and those rules stand. What this class
 * answers is narrower: would the game itself complain about this pair?
 * <p>
 * Groups are compared within one section only, as the tests were. Whether the sections ever conflict with each other is
 * unresolved (EliteDangerous-ConflictRules.md, section 3). A control with no group in the name table has no answer here.
 * <p>
 * The game's answer is mirrored even where it is known to be wrong about play: Multi-Crew conflicts with nearly the
 * whole cockpit although nobody sits in both seats. The game still complains, so the commander is told before it does.
 */
final class GameConflictMatrix {

    private static final Set<String> WARNING_PAIRS = new HashSet<>();

    static {
        warn(BindingSection.GENERAL, "Interface Mode",
                "Holo-Me", "Playlist", "Store Camera");
        warn(BindingSection.GENERAL, "Galaxy Map",
                "Playlist");
        warn(BindingSection.GENERAL, "Camera Suite",
                "Holo-Me", "Playlist", "Store Camera");
        warn(BindingSection.GENERAL, "Free Camera",
                "Holo-Me", "Playlist", "Store Camera");
        warn(BindingSection.GENERAL, "Holo-Me",
                "Playlist");
        warn(BindingSection.SHIP, "Mouse Controls",
                "Flight Rotation", "Flight Thrust", "Alternate Flight Controls", "Flight Throttle", "Flight Landing Overrides", "Flight Miscellaneous", "Targeting", "Weapons", "Cooling", "Miscellaneous", "Mode Switches", "Multi-Crew", "Fighter Orders");
        warn(BindingSection.SHIP, "Flight Rotation",
                "Flight Thrust", "Flight Throttle", "Flight Miscellaneous", "Targeting", "Weapons", "Cooling", "Miscellaneous", "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Flight Thrust",
                "Flight Throttle", "Flight Miscellaneous", "Targeting", "Weapons", "Cooling", "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Alternate Flight Controls",
                "Flight Miscellaneous", "Targeting", "Weapons", "Cooling", "Miscellaneous", "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Flight Throttle",
                "Flight Miscellaneous", "Targeting", "Weapons", "Cooling", "Miscellaneous", "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Flight Landing Overrides",
                "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Flight Miscellaneous",
                "Targeting", "Weapons", "Cooling", "Miscellaneous", "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Targeting",
                "Weapons", "Cooling", "Miscellaneous", "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Weapons",
                "Cooling", "Miscellaneous", "Mode Switches", "Multi-Crew", "Detailed Surface Scanner");
        warn(BindingSection.SHIP, "Cooling",
                "Miscellaneous", "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Miscellaneous",
                "Mode Switches", "Multi-Crew");
        warn(BindingSection.SHIP, "Mode Switches",
                "Headlook Mode", "Multi-Crew", "Detailed Surface Scanner");
        warn(BindingSection.SHIP, "Headlook Mode",
                "Multi-Crew");
        warn(BindingSection.SRV, "Driving",
                "Driving Targeting", "Drive Throttle", "Driving Miscellaneous", "Driving Mode Switches");
        warn(BindingSection.SRV, "Driving Targeting",
                "Drive Throttle", "Driving Miscellaneous", "Driving Mode Switches");
        warn(BindingSection.SRV, "Driving Turret Controls",
                "Driving Mode Switches");
        warn(BindingSection.SRV, "Drive Throttle",
                "Driving Miscellaneous", "Driving Mode Switches");
        warn(BindingSection.SRV, "Driving Miscellaneous",
                "Driving Mode Switches");
        warn(BindingSection.ON_FOOT, "On Foot",
                "On Foot Mode Switches", "On Foot Emotes");
        warn(BindingSection.ON_FOOT, "On Foot Mode Switches",
                "On Foot Emotes");
    }

    private GameConflictMatrix() {
    }

    private static void warn(BindingSection section, String group, String... others) {
        for (String other : others) {
            WARNING_PAIRS.add(key(section, group, other));
        }
    }

    /**
     * Whether the game warns when these two controls share a key. False for two in one group: the tests compared
     * different groups only, so that pair has no answer here.
     */
    static boolean warns(String actionA, String actionB) {
        BindingDisplayNames.ControlName a = BindingDisplayNames.lookup(actionA);
        BindingDisplayNames.ControlName b = BindingDisplayNames.lookup(actionB);
        return a.section() == b.section() && a.group() != null && b.group() != null
                && warns(a.section(), a.group(), b.group());
    }

    /**
     * The same question by group, as the matrix states it.
     */
    static boolean warns(BindingSection section, String groupA, String groupB) {
        return WARNING_PAIRS.contains(key(section, groupA, groupB));
    }

    // Order-independent, so each pair is stored once.
    private static String key(BindingSection section, String groupA, String groupB) {
        return groupA.compareTo(groupB) <= 0
                ? section + "|" + groupA + "|" + groupB
                : section + "|" + groupB + "|" + groupA;
    }
}
