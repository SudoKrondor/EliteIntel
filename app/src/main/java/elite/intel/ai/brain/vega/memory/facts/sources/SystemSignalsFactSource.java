package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.ai.brain.vega.memory.facts.LocalizedFactRelevance;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSource;
import elite.intel.ai.brain.vega.memory.facts.RegisterMemoryFactSource;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.signals.SystemSignals;
import elite.intel.session.PlayerSession;

import java.util.*;

/**
 * Query-relevant fact source for what the FSS has found in the current system: the standing points of interest -
 * resource extraction sites, conflict zones, nav beacons, megaships, installations - as one counted, compact line.
 * It reads the same {@code detectedSignals} the {@code query_signals_in_star_system} briefing does, so the grounding
 * line and the full briefing cannot disagree; the briefing keeps the detail, this keeps the model from guessing.
 * <p>
 * What is in a system is what decides whether there is anything to do in it - a hazardous extraction site for
 * bounty hunting, a conflict zone for mercenary work, a beacon to scan for exploration data - so the subjects it
 * admits itself for are the signal questions, not the location ones: where the commander is already has its own
 * sources.
 *
 * <h2>Transient signals are deliberately left out</h2>
 * Unidentified signal sources (high grade emissions among them) carry only a {@code TimeRemaining} countdown, and
 * nothing records when the app first saw one, so a stored USS cannot be aged out - it would still be reported as
 * present hours later, in a later session. A signal the commander cannot fly to is worse than no fact at all, so
 * only the standing signals are reported here. Fleet and squadron carriers are counted rather than named, because
 * their names are noise and the count is the part that says how busy the system is.
 */
@RegisterMemoryFactSource
public final class SystemSignalsFactSource implements MemoryFactSource {

    /**
     * Provenance label for the {@code <fact source="...">} attribute.
     */
    private static final String ID = "signals";
    private static final List<String> RELEVANCE_ALIAS_KEYS = List.of("query_signals_in_star_system");

    /**
     * Transient signal type: a countdown with no recorded start, so a stored one cannot be trusted as present.
     */
    private static final String TRANSIENT_TYPE = "USS";

    /**
     * The one label this class writes itself rather than taking from the game. It is counted rather than listed, so
     * unlike every other entry it is a category noun and has to read correctly in both numbers.
     */
    private static final String CARRIER = "fleet carrier";

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
        Long systemAddress = PlayerSession.getInstance().getLocationData().getSystemAddress();
        if (systemAddress == null) {
            return List.of();
        }
        Collection<LocationDto> locations = LocationManager.getInstance().findAllBySystemAddress(systemAddress);
        String fact = format(counted(locations));
        return fact.isBlank() ? List.of() : List.of(fact);
    }

    /**
     * Counts the standing signals of the system by their displayed name, carriers collapsed into one entry. The
     * whole system's records are read together because a signal is recorded against whichever body's record was
     * current when the FSS reported it - and for the same reason they are counted through
     * {@link SystemSignals#distinct}, so a signal filed against two bodies is one signal and not two. That
     * matters most for the carriers, which are the entry reported as a bare number: without it, one carrier
     * honked twice reads as two carriers.
     */
    static Map<String, Integer> counted(Collection<LocationDto> locations) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SystemSignals.Sighting sighting : SystemSignals.distinct(locations)) {
            String label = label(sighting.signal());
            if (label != null) {
                counts.merge(label, 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * How one signal is named in the line, or null when it does not belong in it. Prefers the game's own localised
     * name because the raw name is a {@code $...;} symbol, which is an identifier and must never be spoken.
     */
    private static String label(FssSignalDto signal) {
        String type = signal.getSignalType();
        if (type != null && TRANSIENT_TYPE.equalsIgnoreCase(type.trim())) {
            return null;
        }
        if (isCarrier(type)) {
            return CARRIER;
        }
        String localised = FactLine.value(signal.getSignalNameLocalised());
        if (localised != null) {
            return localised;
        }
        String raw = FactLine.value(signal.getSignalName());
        return raw == null || raw.startsWith("$") ? null : raw;
    }

    private static boolean isCarrier(String type) {
        return "FleetCarrier".equalsIgnoreCase(type) || "SquadronCarrier".equalsIgnoreCase(type);
    }

    /**
     * Builds the single signals line, the most numerous first so the shared line cap drops the rarest rather than
     * the busiest. Pure and package-visible for testing.
     */
    static String format(Map<String, Integer> counts) {
        if (counts.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .forEach(entry -> parts.add(render(entry.getKey(), entry.getValue())));
        return FactLine.capped("signals detected in this system", parts);
    }

    /**
     * One entry as it reads on the line. The carrier count is a category this class names, so it takes a plural;
     * every other label is the game's own name for a single signal, where "3 x {@code <name>}" is the honest form
     * because pluralizing a proper name would be inventing one.
     */
    private static String render(String label, int count) {
        if (count == 1) {
            return label;
        }
        return CARRIER.equals(label) ? count + " fleet carriers" : count + " x " + label;
    }
}
