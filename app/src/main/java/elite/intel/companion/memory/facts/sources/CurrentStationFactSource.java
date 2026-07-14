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
import java.util.Set;

/**
 * Context-gated fact source for the station the commander is at: it contributes only when the current situation is
 * docked or on foot in a station (classified by {@link Status#getSituation}), and grounds the companion in that
 * station's character (type, economy, controlling faction, number of services) as one compact, length-capped line.
 * It reads the current location record ({@link LocationManager}) the same way {@code query_station_details} does.
 * The source admits itself for station-detail/location subjects; it stays silent away from a station and
 * coexists with {@link CurrentSystemFactSource} under the block's per-source and total caps.
 */
@RegisterMemoryFactSource
public final class CurrentStationFactSource implements MemoryFactSource {

    /** Provenance label for the {@code <fact source="...">} attribute. */
    private static final String ID = "station";
    private static final List<String> RELEVANCE_ALIAS_KEYS = List.of(
            "query_current_location",
            "query_station_details");

    /** The situations that count as being at a station; away from these the source stays silent. */
    private static final Set<PlayerSituation> AT_STATION = EnumSet.of(
            PlayerSituation.IN_SHIP_DOCKED,
            PlayerSituation.ON_FOOT_STATION,
            PlayerSituation.ON_FOOT_HANGAR,
            PlayerSituation.ON_FOOT_SOCIAL);

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
        if (!AT_STATION.contains(Status.getInstance().getSituation(null))) {
            return List.of();
        }
        LocationDto station = LocationManager.getInstance().findByLocationData(PlayerSession.getInstance().getLocationData());
        String fact = format(FactLine.value(station.getStationName()), station.getStationType(),
                station.getStationEconomy(), station.getStationFaction(), servicesCount(station.getStationServices()));
        return fact.isBlank() ? List.of() : List.of(fact);
    }

    /**
     * Builds the single compact station line from the station name and its attributes: skips empty/unknown fields and
     * appends parts (highest-value first) capped in length by {@link FactLine#capped}. Returns empty only when there
     * is neither a name nor any attribute. Pure and package-visible for testing.
     */
    static String format(String name, String type, String economy, String faction, int services) {
        List<String> parts = new ArrayList<>();
        addIf(parts, FactLine.value(type));
        addIf(parts, prefix("economy", FactLine.value(economy)));
        addIf(parts, prefix("faction", FactLine.value(faction)));
        if (services > 0) {
            parts.add(services + (services == 1 ? " service" : " services"));
        }

        boolean hasName = name != null && !name.isBlank();
        if (!hasName && parts.isEmpty()) {
            return "";
        }
        String head = hasName ? "current station " + name.strip() : "current station";
        return FactLine.capped(head, parts);
    }

    private static int servicesCount(List<String> services) {
        return services == null ? 0 : services.size();
    }

    private static void addIf(List<String> parts, String value) {
        if (value != null) {
            parts.add(value);
        }
    }

    private static String prefix(String label, String value) {
        return value == null ? null : label + " " + value;
    }
}
