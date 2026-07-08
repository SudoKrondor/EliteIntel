package elite.intel.companion.memory.facts.sources;

import elite.intel.companion.memory.facts.MemoryFactContext;
import elite.intel.companion.memory.facts.MemoryFactSource;
import elite.intel.companion.memory.facts.RegisterMemoryFactSource;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Always-on fact source for who the commander is, grounding the companion in the person it serves: name, allegiance,
 * the career ranks (combat, exploration, exobiology, mercenary), any powerplay pledge, and current credit balance.
 * It ignores the query, contributes a single compact, length-capped line, and stays silent when the commander name is
 * unknown. Rank fields default to the journal's {@code "unknown"} placeholder and are then dropped, so an early
 * session (no rank data yet) still yields a clean name-only line.
 */
@RegisterMemoryFactSource
public final class CommanderProfileFactSource implements MemoryFactSource {

    /** Provenance label for the {@code <fact source="...">} attribute. */
    private static final String ID = "commander";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<String> factsFor(MemoryFactContext context) {
        PlayerSession session = PlayerSession.getInstance();
        RankAndProgressDto ranks = session.getRankAndProgressDto();
        String fact = ranks == null
                ? format(session.getPlayerName(), null, null, null, null, null, null, session.getPersonalCredits())
                : format(session.getPlayerName(), ranks.getAllegiance(), ranks.getCombatRank(),
                        ranks.getExplorationRank(), ranks.getExobiologyRank(), ranks.getMercenaryRank(),
                        ranks.getPledgedToPower(), session.getPersonalCredits());
        return fact.isBlank() ? List.of() : List.of(fact);
    }

    /**
     * Builds the commander line: the name is the head; the rest follow in priority order (highest-value first) so the
     * shared length cap drops the least useful tail first - the headline allegiance, combat and exploration ranks,
     * credits and powerplay pledge stay, while the niche exobiology and mercenary ranks are appended only if they still
     * fit. Skips empty/unknown fields (career ranks default to the {@code "unknown"} placeholder) and non-positive
     * credits. Empty when the name is unknown. Pure and package-visible for testing.
     */
    static String format(String name, String allegiance, String combatRank, String explorationRank,
                         String exobiologyRank, String mercenaryRank, String pledgedPower, long credits) {
        if (isUnknown(name)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        addPart(parts, "allegiance", FactLine.value(allegiance));
        addPart(parts, "combat rank", FactLine.value(combatRank));
        addPart(parts, "exploration rank", FactLine.value(explorationRank));
        addPart(parts, "credits", credits(credits));
        addPart(parts, "pledged to", FactLine.value(pledgedPower));
        addPart(parts, "exobiology rank", FactLine.value(exobiologyRank));
        addPart(parts, "mercenary rank", FactLine.value(mercenaryRank));
        return FactLine.capped("commander " + name.strip(), parts);
    }

    private static void addPart(List<String> parts, String label, String value) {
        if (value != null) {
            parts.add(label + " " + value);
        }
    }

    /** Credit balance in a compact form (12.3B cr / 1.2M cr / 340K cr), or null when non-positive. */
    private static String credits(long credits) {
        if (credits <= 0) {
            return null;
        }
        if (credits >= 1_000_000_000L) {
            return round(credits / 1_000_000_000.0) + "B cr";
        }
        if (credits >= 1_000_000L) {
            return round(credits / 1_000_000.0) + "M cr";
        }
        if (credits >= 1_000L) {
            return round(credits / 1_000.0) + "K cr";
        }
        return credits + " cr";
    }

    /** One decimal place, trimming a trailing {@code .0}. */
    private static String round(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    private static boolean isUnknown(String name) {
        return name == null || name.isBlank() || "unknown".equalsIgnoreCase(name.strip());
    }
}
