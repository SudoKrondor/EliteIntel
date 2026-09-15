package elite.intel.gameapi.signals;

import java.util.Locale;

/**
 * A war an arrival's Conflicts block reports as being fought in the system right now.
 * <p>
 * Only these spawn conflict zones. An election is a conflict in the game's terms but is settled
 * with missions and trade, not guns, and a war still {@code pending} has no zones yet - so both
 * are read as "no war here" by {@link #of}.
 */
public record ActiveWar(String warType, String faction1, String faction2) {

    /**
     * The war these Conflicts fields describe, or null when they describe no war worth flying to.
     */
    public static ActiveWar of(String warType, String status, String faction1, String faction2) {
        if (warType == null || status == null) return null;
        if (!"active".equalsIgnoreCase(status)) return null;
        String kind = warType.toLowerCase(Locale.ROOT);
        if (!kind.equals("war") && !kind.equals("civilwar")) return null;
        if (isBlank(faction1) || isBlank(faction2)) return null;
        return new ActiveWar(kind, faction1, faction2);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
