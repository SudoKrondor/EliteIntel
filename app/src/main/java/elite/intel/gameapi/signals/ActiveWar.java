package elite.intel.gameapi.signals;

import elite.intel.gameapi.journal.events.FSDJumpEvent;

import java.util.List;
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
     * The first war being fought in a system, from its arrival's Conflicts block, or null when none is.
     * A system can host two at once, and then the zones belong to both and one pair of names is as good
     * as the other for telling the commander what to expect. The one reading of the block, whether it
     * came off the commander's own journal or the EDDN relay.
     */
    public static ActiveWar firstIn(List<FSDJumpEvent.Conflict> conflicts) {
        if (conflicts == null) return null;
        for (FSDJumpEvent.Conflict conflict : conflicts) {
            if (conflict == null) continue;
            ActiveWar war = of(conflict.getWarType(), conflict.getStatus(),
                    conflict.getFaction1() == null ? null : conflict.getFaction1().getName(),
                    conflict.getFaction2() == null ? null : conflict.getFaction2().getName());
            if (war != null) return war;
        }
        return null;
    }

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
