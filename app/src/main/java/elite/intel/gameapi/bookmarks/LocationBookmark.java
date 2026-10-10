package elite.intel.gameapi.bookmarks;

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
 * @param displayName what the commander renamed it to; null until they do, and the card shows the place
 */
public record LocationBookmark(long id, Kind kind, String starSystem, String stationName, String planetName,
                               Double latitude, Double longitude, String displayName) {

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
        return new LocationBookmark(0, Kind.SYSTEM, starSystem, null, null, null, null, null);
    }

    public static LocationBookmark station(String starSystem, String stationName) {
        return new LocationBookmark(0, Kind.STATION, starSystem, stationName, null, null, null, null);
    }

    public static LocationBookmark planetaryPort(String starSystem, String planetName, String stationName) {
        return new LocationBookmark(0, Kind.PLANETARY_PORT, starSystem, stationName, planetName, null, null, null);
    }

    public static LocationBookmark planet(String starSystem, String planetName) {
        return new LocationBookmark(0, Kind.PLANET, starSystem, null, planetName, null, null, null);
    }

    public static LocationBookmark surface(String starSystem, String planetName, double latitude, double longitude) {
        return new LocationBookmark(0, Kind.SURFACE, starSystem, null, planetName, latitude, longitude, null);
    }

    public LocationBookmark withId(long id) {
        return new LocationBookmark(id, kind, starSystem, stationName, planetName, latitude, longitude, displayName);
    }

    /**
     * The same place under the commander's own name. A blank name takes the rename back.
     */
    public LocationBookmark withDisplayName(String name) {
        return new LocationBookmark(id, kind, starSystem, stationName, planetName, latitude, longitude,
                storedName(name));
    }

    /**
     * A name as it is kept: trimmed, and null when there is nothing left, which means "not renamed".
     */
    public static String storedName(String name) {
        return name == null || name.isBlank() ? null : name.strip();
    }

    public boolean isRenamed() {
        return displayName != null;
    }

    /**
     * What VEGA calls it: the commander's name for it when they gave one, else the place. Never the
     * coordinates, which the card shows but nobody wants read aloud.
     */
    public String spokenName() {
        return isRenamed() ? displayName : placeName();
    }

    public boolean hasCoordinates() {
        return kind == Kind.SURFACE && latitude != null && longitude != null;
    }

    /**
     * The place in words, as VEGA says it: the port, the planet or the system. A planetary port is named by its
     * station alone - VEGA names the planet where it matters, when plotting the way there - and the card adds
     * the planet for reading (see {@code LocationBookmarkCard.label}).
     */
    public String placeName() {
        return switch (kind) {
            case SYSTEM -> starSystem;
            case STATION, PLANETARY_PORT -> stationName;
            case PLANET, SURFACE -> planetName;
        };
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
