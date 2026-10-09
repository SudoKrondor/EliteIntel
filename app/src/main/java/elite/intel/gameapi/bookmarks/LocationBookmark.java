package elite.intel.gameapi.bookmarks;

import elite.intel.util.StringUtls;

import java.util.Locale;

/**
 * A place the commander asked to remember, as precisely as where they were allows.
 * <p>
 * The {@link Kind} says which fields mean anything: every kind names its star system, and each step down
 * the list names one more thing about where in it. Navigation always plots to the star system - the galaxy
 * map knows nothing finer - and only a {@link Kind#SURFACE} spot carries coordinates for surface guidance.
 *
 * @param id          row id; 0 for a bookmark not yet saved
 * @param stationName the port, for {@link Kind#STATION} and {@link Kind#PLANETARY_PORT}; else null
 * @param planetName  the body's full name, for the planetary kinds; else null
 * @param latitude    degrees, for {@link Kind#SURFACE}; else null
 * @param longitude   degrees, for {@link Kind#SURFACE}; else null
 */
public record LocationBookmark(long id, Kind kind, String starSystem, String stationName, String planetName,
                               Double latitude, Double longitude) {

    /**
     * Two surface spots closer than this, in degrees on both axes, are the same spot. About a hundred metres on
     * an Earth-sized world: a commander who asks twice without moving is not making a second bookmark.
     */
    static final double SAME_SPOT_DEGREES = 0.001;

    public enum Kind {
        /**
         * In supercruise, deep space, or on a carrier - a carrier moves, so only the system is a place.
         */
        SYSTEM,
        /**
         * Docked at an orbital station.
         */
        STATION,
        /**
         * Docked at a port on a planet's surface.
         */
        PLANETARY_PORT,
        /**
         * Near a planet but still flying - orbital cruise, glide or normal space above it.
         */
        PLANET,
        /**
         * On the ground: landed, in an SRV or on foot.
         */
        SURFACE
    }

    public static LocationBookmark system(String starSystem) {
        return new LocationBookmark(0, Kind.SYSTEM, starSystem, null, null, null, null);
    }

    public static LocationBookmark station(String starSystem, String stationName) {
        return new LocationBookmark(0, Kind.STATION, starSystem, stationName, null, null, null);
    }

    public static LocationBookmark planetaryPort(String starSystem, String planetName, String stationName) {
        return new LocationBookmark(0, Kind.PLANETARY_PORT, starSystem, stationName, planetName, null, null);
    }

    public static LocationBookmark planet(String starSystem, String planetName) {
        return new LocationBookmark(0, Kind.PLANET, starSystem, null, planetName, null, null);
    }

    public static LocationBookmark surface(String starSystem, String planetName, double latitude, double longitude) {
        return new LocationBookmark(0, Kind.SURFACE, starSystem, null, planetName, latitude, longitude);
    }

    public LocationBookmark withId(long id) {
        return new LocationBookmark(id, kind, starSystem, stationName, planetName, latitude, longitude);
    }

    public boolean hasCoordinates() {
        return kind == Kind.SURFACE && latitude != null && longitude != null;
    }

    /**
     * The place in words, without coordinates: what the card leads with and what VEGA says aloud. A planetary
     * port is named by its short body designation ("A 2") because the system name in front of it adds nothing
     * the station name does not.
     */
    public String placeName() {
        return switch (kind) {
            case SYSTEM -> starSystem;
            case STATION -> stationName;
            case PLANETARY_PORT -> shortPlanetName() + " " + stationName;
            case PLANET, SURFACE -> planetName;
        };
    }

    /**
     * The body's name with the system's taken off the front, or the full name when the body is not named after
     * its system (Earth in Sol, say).
     */
    public String shortPlanetName() {
        String shortName = StringUtls.subtractString(planetName, starSystem);
        return shortName.isBlank() ? planetName : shortName;
    }

    /**
     * Whether this names the same place as another bookmark, ignoring ids.
     */
    public boolean samePlaceAs(LocationBookmark other) {
        if (kind != other.kind) return false;
        if (!sameText(starSystem, other.starSystem)) return false;
        if (!sameText(stationName, other.stationName)) return false;
        if (!sameText(planetName, other.planetName)) return false;
        if (!hasCoordinates()) return true;
        return Math.abs(latitude - other.latitude) < SAME_SPOT_DEGREES
                && Math.abs(longitude - other.longitude) < SAME_SPOT_DEGREES;
    }

    private static boolean sameText(String a, String b) {
        if (a == null || b == null) return a == b;
        return a.trim().toLowerCase(Locale.ROOT).equals(b.trim().toLowerCase(Locale.ROOT));
    }
}
