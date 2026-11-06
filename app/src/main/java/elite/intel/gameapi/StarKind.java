package elite.intel.gameapi;

import java.util.Locale;

/**
 * The families of star class worth telling apart on a route, read from the game's {@code StarClass} code.
 * <p>
 * Whether a star can be scooped is a separate question, answered by {@code KGBFOAM} where the route is plotted.
 * Everything here is simply not ordinary: a neutron star or a white dwarf is a jet-cone boost for the FSD, a
 * Wolf-Rayet star is dangerously hot, and the rest are rare enough to mention.
 */
public enum StarKind {

    /**
     * Jet-cone boost for the FSD, x4.
     */
    NEUTRON,
    /**
     * Jet-cone boost for the FSD, x1.5.
     */
    WHITE_DWARF,
    /**
     * Very hot and massive.
     */
    WOLF_RAYET,
    /**
     * Exotic stars, rogue planets and the nebula classes.
     */
    RARE,
    /**
     * Every other class, and a star whose class is not known.
     */
    ORDINARY;

    public static StarKind of(String starClass) {
        if (starClass == null) return ORDINARY;
        return switch (starClass.toUpperCase(Locale.ROOT)) {
            case "N" -> NEUTRON;
            case "D", "DA", "DAB", "DAO", "DAZ", "DAV", "DB", "DBZ", "DBV", "DO", "DOV", "DQ", "DC", "DCV", "DX" ->
                    WHITE_DWARF;
            case "W", "WN", "WNC", "WC", "WO" -> WOLF_RAYET;
            case "X", "ROGUEPLANET", "NEBULA", "STELLARREMNANTNEBULA" -> RARE;
            default -> ORDINARY;
        };
    }
}
