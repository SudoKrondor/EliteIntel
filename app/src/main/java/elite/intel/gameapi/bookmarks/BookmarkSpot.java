package elite.intel.gameapi.bookmarks;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;

import java.util.Optional;

/**
 * Works out what "bookmark this" means from where the commander is.
 * <p>
 * The rule is: as precise as the situation allows, and no more.
 * <ul>
 *   <li>On a pad, the port - unless it is a carrier, which will not be there next week, so only the system.
 *       A port with a surface position under it is a planetary port and names its body too.</li>
 *   <li>On the ground (landed, SRV, on foot) the exact spot, so surface guidance can bring them back.</li>
 *   <li>Flying near a planet - orbital cruise, glide, or normal space over it - the planet only. The game does
 *       report a position there, but the ship is moving, so it marks nothing anyone would come back to.</li>
 *   <li>Anywhere else, the system.</li>
 * </ul>
 */
public final class BookmarkSpot {

    /**
     * What the resolver reads, gathered in one place so the rules can be tested without a game running.
     *
     * @param stationName      the port the ship is on, or null when it is not on one
     * @param stationIsCarrier whether that port is a fleet or squadron carrier
     * @param bodyName         the body Status.json reports position against, or null
     * @param hasLatLong       whether Status.json reports a surface position
     */
    public record Here(PlayerSituation situation, String starSystem, String stationName, boolean stationIsCarrier,
                       String bodyName, boolean hasLatLong, double latitude, double longitude) {
    }

    private BookmarkSpot() {
    }

    /**
     * The bookmark for where the commander is right now, or empty before the app knows which system that is.
     */
    public static Optional<LocationBookmark> current() {
        return resolve(liveHere());
    }

    static Optional<LocationBookmark> resolve(Here here) {
        if (blank(here.starSystem())) return Optional.empty();
        String system = here.starSystem();
        boolean onBody = here.hasLatLong() && !blank(here.bodyName());

        return Optional.of(switch (here.situation()) {
            case IN_SHIP_DOCKED, ON_FOOT_STATION, ON_FOOT_HANGAR, ON_FOOT_SOCIAL -> {
                if (blank(here.stationName()) || here.stationIsCarrier()) yield LocationBookmark.system(system);
                yield onBody
                        ? LocationBookmark.planetaryPort(system, here.bodyName(), here.stationName())
                        : LocationBookmark.station(system, here.stationName());
            }
            case IN_SHIP_LANDED, IN_SRV, ON_FOOT_PLANET, ON_FOOT -> onBody
                    ? LocationBookmark.surface(system, here.bodyName(), here.latitude(), here.longitude())
                    : LocationBookmark.system(system);
            case IN_SHIP_GLIDE, IN_SHIP_SUPERCRUISE, IN_SHIP_ORBIT, IN_FIGHTER -> onBody
                    ? LocationBookmark.planet(system, here.bodyName())
                    : LocationBookmark.system(system);
            case IN_TAXI, IN_SHIP_RING, IN_SHIP_DEEP_SPACE, UNKNOWN -> LocationBookmark.system(system);
        });
    }

    private static Here liveHere() {
        Status status = Status.getInstance();
        GameEvents.StatusEvent snapshot = status.getStatus();
        DockedMarket docked = DockedMarket.getInstance();

        String stationName = null;
        boolean carrier = false;
        if (docked.marketId() != 0) {
            LocationDto station = LocationManager.getInstance().findCurrentStation();
            stationName = blank(docked.stationName()) ? station.getStationName() : docked.stationName();
            // The callsign check is a backstop: a squadron carrier docks under the fleet carrier station type as
            // far as anyone has logged, but our own squadron's carrier we can name for certain.
            String squadronCallSign = PlayerSession.getInstance().getSquadronCarrierData().getCallSign();
            carrier = station.getLocationType() == LocationDto.LocationType.FLEET_CARRIER
                    || (!blank(squadronCallSign) && squadronCallSign.equalsIgnoreCase(stationName));
        }
        return new Here(status.getSituation(null), PlayerSession.getInstance().getPrimaryStarName(),
                stationName, carrier, snapshot.getBodyName(), status.hasLatLong(),
                snapshot.getLatitude(), snapshot.getLongitude());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
