package elite.intel.gameapi.population;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * How big a port is, for sharing a star system's population out among its ports.
 * <p>
 * The game publishes population for the star system only: no journal event, nor EDSM or Spansh, says how many
 * people live in one station. The tiers are the project owner's (2026-10-21) and the head counts are invented to
 * fit them. They drive an estimate told for immersion, not game data any strategy may rest on.
 * <p>
 * A port's capacity is both its weight in the share and its ceiling: in a system of billions, a lone outpost
 * is full rather than holding billions.
 */
public enum PortSize {
    HUGE(10_000_000),
    LARGE(2_000_000),
    MEDIUM(200_000),
    SMALL(10_000);

    /**
     * The journal's names for a port, keyed lower case. Both spellings are here because they arrive from
     * two events: {@code Docked} gives a {@code StationType}, the FSS gives a {@code SignalType}.
     * <p>
     * Left out on purpose: fleet and squadron carriers (a crew, not a population), megaships, construction
     * depots and installations. {@code SurfaceStation} is absent too: colony-built stations of every shape
     * dock under that one name (a Coriolis and an asteroid base among them), so it says nothing of size.
     * See {@link SystemPort#size()} for how such a port is sized.
     */
    private static final Map<String, PortSize> BY_TYPE = Map.ofEntries(
            Map.entry("orbis", HUGE),
            Map.entry("stationoneilorbis", HUGE),
            Map.entry("stationoneilcylinder", HUGE),
            Map.entry("ocellus", LARGE),
            Map.entry("bernal", LARGE),
            Map.entry("stationbernalsphere", LARGE),
            Map.entry("coriolis", LARGE),
            Map.entry("stationcoriolis", LARGE),
            Map.entry("dodec", LARGE),
            Map.entry("stationdodec", LARGE),
            Map.entry("asteroidbase", LARGE),
            Map.entry("stationasteroid", LARGE),
            Map.entry("craterport", LARGE),
            Map.entry("outpost", MEDIUM),
            Map.entry("crateroutpost", MEDIUM),
            Map.entry("onfootsettlement", SMALL));

    /**
     * The journal's type for a colony-built station, whatever its shape.
     */
    static final String SURFACE_STATION = "surfacestation";

    private final long capacity;

    PortSize(long capacity) {
        this.capacity = capacity;
    }

    public long capacity() {
        return capacity;
    }

    /**
     * The size of a port of this journal type, empty when the type is not a port people live in or names no size.
     */
    public static Optional<PortSize> of(String journalType) {
        if (journalType == null) return Optional.empty();
        return Optional.ofNullable(BY_TYPE.get(journalType.toLowerCase(Locale.ROOT)));
    }
}
