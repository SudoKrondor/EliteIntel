package elite.intel.companion.memory.facts.sources;

import elite.intel.companion.memory.facts.LocalizedFactRelevance;
import elite.intel.companion.memory.facts.MemoryFactContext;
import elite.intel.companion.memory.facts.MemoryFactSource;
import elite.intel.companion.memory.facts.RegisterMemoryFactSource;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Query-relevant fact source for the current star system, grounding the companion in where the commander is. While
 * travelling the system (supercruise or deep space) it gives the system's full character (allegiance, security,
 * economy, population, controlling power); once focused on a body or station - where {@link CurrentBodyFactSource}
 * (and later a station source) carry the detail - it shrinks to a short grounding line (system, allegiance, security)
 * so the two coexist without crowding the lean block. The source admits itself for current-location/system-security
 * subjects; it contributes a single compact line and stays silent when the current system is unknown.
 */
@RegisterMemoryFactSource
public final class CurrentSystemFactSource implements MemoryFactSource {

    /** Provenance label for the {@code <fact source="...">} attribute. */
    private static final String ID = "system";
    private static final List<String> RELEVANCE_ALIAS_KEYS = List.of(
            "query_current_location",
            "query_system_security");

    /**
     * Situations where the system itself is the commander's frame, so the full system line is warranted. Anything else
     * (docked, landed, near a body, on foot) means a more specific source carries the detail, so the system line
     * shrinks to a grounding summary. UNKNOWN defaults to full (safe grounding).
     */
    private static final Set<PlayerSituation> SYSTEM_TRAVEL = EnumSet.of(
            PlayerSituation.IN_SHIP_SUPERCRUISE,
            PlayerSituation.IN_SHIP_DEEP_SPACE,
            PlayerSituation.IN_FIGHTER,
            PlayerSituation.IN_TAXI,
            PlayerSituation.UNKNOWN);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isRelevant(MemoryFactContext context) {
        return LocalizedFactRelevance.matches(context, 2, RELEVANCE_ALIAS_KEYS);
    }

    @Override
    public List<String> factsFor(MemoryFactContext context) {
        String system = PlayerSession.getInstance().getPrimaryStarName();
        if (isUnknown(system)) {
            return List.of();
        }
        boolean brief = !SYSTEM_TRAVEL.contains(Status.getInstance().getSituation(null));
        LocationDto loc = LocationManager.getInstance().findPrimaryStar(system);
        String fact = format(brief, system, loc.getAllegiance(), loc.getSecurity(), loc.getEconomy(),
                loc.getPopulation(), loc.getControllingPower());
        return fact.isBlank() ? List.of() : List.of(fact);
    }

    /**
     * Builds the system line: identity, allegiance and security always appear; economy, population and controlling
     * power are added only in the full (not {@code brief}) form used while travelling the system. Skips empty/unknown
     * fields, cleans the odd raw security token, and length-caps via {@link FactLine#capped}. Pure and package-visible
     * for testing.
     */
    static String format(boolean brief, String system, String allegiance, String security, String economy,
                         long population, String power) {
        if (isUnknown(system)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        addPart(parts, "allegiance", FactLine.value(allegiance));
        addPart(parts, "security", security(security));
        if (!brief) {
            addPart(parts, "economy", FactLine.value(economy));
            addPart(parts, "population", population(population));
            addPart(parts, "controlled by", FactLine.value(power));
        }
        return FactLine.capped("current system " + system.strip(), parts);
    }

    private static void addPart(List<String> parts, String label, String value) {
        if (value != null) {
            parts.add(label + " " + value);
        }
    }

    /**
     * The security level cleaned for display: some journal paths store the raw token {@code $SYSTEM_SECURITY_high;}
     * instead of the localised "High Security", so unwrap the token to its level and drop a redundant trailing
     * "Security" word. Null when empty/unknown.
     */
    private static String security(String raw) {
        String v = FactLine.value(raw);
        if (v == null) {
            return null;
        }
        if (v.startsWith("$") && v.endsWith(";")) {
            String token = v.substring(0, v.length() - 1);
            v = token.substring(token.lastIndexOf('_') + 1); // "$SYSTEM_SECURITY_high;" -> "high"
        }
        v = v.replaceAll("(?i)\\s*security$", "").strip(); // "High Security" -> "High"
        return v.isEmpty() ? null : v;
    }

    /** Population in a compact form (22.7B / 1.2M / 340K), or null when unpopulated. */
    private static String population(long population) {
        if (population <= 0) {
            return null;
        }
        if (population >= 1_000_000_000L) {
            return round(population / 1_000_000_000.0) + "B";
        }
        if (population >= 1_000_000L) {
            return round(population / 1_000_000.0) + "M";
        }
        if (population >= 1_000L) {
            return round(population / 1_000.0) + "K";
        }
        return Long.toString(population);
    }

    /** One decimal place, trimming a trailing {@code .0}. */
    private static String round(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    private static boolean isUnknown(String system) {
        return system == null || system.isBlank() || "unknown".equalsIgnoreCase(system.strip());
    }
}
