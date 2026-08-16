package elite.intel.db.managers;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.dao.TradeProfileDao;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.spansh.station.StationSearchClient;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.session.PlayerSession;
import elite.intel.util.NavigationUtils;
import elite.intel.util.ShipPadSizes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class TradeProfileManager {

    public static final int MAX_DISTANCE_TO_INITIAL_STATION = 500;
    /**
     * How far out of our own records a starting station may be picked before we ask Spansh instead.
     */
    public static final int MAX_DISTANCE_TO_KNOWN_STATION_LY = 20;
    private static final Logger log = LogManager.getLogger(TradeProfileManager.class);
    private static TradeProfileManager instance;
    private final ShipManager shipManager = ShipManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private TradeProfileManager() {
    }

    public static synchronized TradeProfileManager getInstance() {
        if (instance == null) {
            instance = new TradeProfileManager();
        }
        return instance;
    }

    public TradeRouteSearchCriteria getCriteria(boolean withStationStartingStation) {
        final ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) return null;

        TradeProfileDao.TradeProfile profile = getProfile(ship);
        TradeRouteSearchCriteria criteria = new TradeRouteSearchCriteria();
        criteria.setAllowPlanetary(profile.isAllowPlanetary());
        criteria.setAllowPermit(profile.isAllowPermit());
        criteria.setAllowProhibited(profile.isAllowProhibited());
        criteria.setAllowFleetCarriers(profile.isAllowFleetCarrier());
        criteria.setMaxCargo(ship.getCargoCapacity());
        criteria.setMaxJumpDistance(((int) playerSession.getShipLoadout().getMaxJumpRange()));
        criteria.setMaxLsFromArrival(profile.getMaxDistanceLs());
        criteria.setMaxJumps(profile.getMaxJumps());
        criteria.setAllowStrongHold(profile.isAllowStrongHold());
        criteria.setRequiresLargePad(shipManager.requireLargePad());
        criteria.setStartingCapital(profile.getStartingBudget());

        if (withStationStartingStation) {
            if (!setStartingStationFromOurRecords(criteria)) {
                if (spanshSearchForStation(criteria)) return null;
            }

            if (criteria.getStation() == null || criteria.getSystem() == null) {
                GameEventBus.publish(new MissionCriticalAnnouncementEvent("Unable to find a suitable initial trade station withing " + MAX_DISTANCE_TO_INITIAL_STATION + " light years."));
                return null;
            }
        }
        log.debug("Trade route criteria: {}", criteria.toString());
        return criteria;
    }

    /**
     * Picks the starting station out of the stations we have already visited: the nearest one within
     * {@link #MAX_DISTANCE_TO_KNOWN_STATION_LY} light years, falling back to the current system when we
     * have no coordinates to measure from. Returns false when our own records cannot supply one, which
     * is the caller's cue to ask Spansh.
     * <p>
     * WHY ours first: a station we have docked at is one we know the ship can use and one the commander
     * can reach, and reading it costs a local query instead of a network round trip.
     */
    private boolean setStartingStationFromOurRecords(TradeRouteSearchCriteria criteria) {
        LocationManager locationManager = LocationManager.getInstance();

        List<LocationDto> candidates = locationManager.findKnownStationsWithin(
                locationManager.getGalacticCoordinates(), MAX_DISTANCE_TO_KNOWN_STATION_LY
        );
        if (candidates.isEmpty()) {
            // No coordinates recorded for the systems we know - fall back to the system we are standing in,
            // which is identified by its address and so needs no coordinates at all.
            candidates = locationManager.findStationsInCurrentStarSystem(
                    playerSession.getLocationData().getSystemAddress()
            );
        }

        return candidates.stream()
                .filter(station -> station.getStationName() != null && station.getStarName() != null)
                .filter(TradeProfileManager::sellsCommodities)
                .findFirst()
                .map(station -> {
                    criteria.setStation(station.getStationName());
                    criteria.setSystem(station.getStarName());
                    log.debug("Starting trade station taken from our own records: {} in {}", station.getStationName(), station.getStarName());
                    return true;
                })
                .orElse(false);
    }

    /**
     * True unless the record positively says the station has no commodity market. A trade route has to
     * start where cargo can be bought, but the older station rows were written before station services
     * were captured, so an unknown service list is taken as usable rather than discarded.
     */
    private static boolean sellsCommodities(LocationDto station) {
        List<String> services = station.getStationServices();
        if (services == null || services.isEmpty()) return true;
        return services.stream().anyMatch(service -> "commodities".equalsIgnoreCase(service));
    }

    private boolean spanshSearchForStation(TradeRouteSearchCriteria criteria) {
        StationSearchClient stationSearchClient = StationSearchClient.getInstance();

        TradeStationSearchCriteria initialStationCriteria = new TradeStationSearchCriteria();
        TradeStationSearchCriteria.ReferenceCoords coords = new TradeStationSearchCriteria.ReferenceCoords();
        LocationManager locationManager = LocationManager.getInstance();
        LocationDao.Coordinates galacticCoordinates = locationManager.getGalacticCoordinates();
        String primaryStarName = playerSession.getPrimaryStarName();

        if (galacticCoordinates == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("Galactic coordinates are not available."));
            return true;
        }

        /// Sol is 0,0,0 but also if we do not have coordinates, our location will be 0,0,0.
        if (galacticCoordinates.x() == 0 && galacticCoordinates.y() == 0 && galacticCoordinates.z() == 0 && !"Sol".equalsIgnoreCase(primaryStarName)) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("Galactic coordinates are not available."));
            return true;
        }

        coords.setX(galacticCoordinates.x());
        coords.setY(galacticCoordinates.y());
        coords.setZ(galacticCoordinates.z());
        initialStationCriteria.setReferenceCoords(coords);
        initialStationCriteria.setPage(0);


        TradeStationSearchCriteria.Filters filters = new TradeStationSearchCriteria.Filters();
        filters.setDistanceToArrival(new TradeStationSearchCriteria.RangeFilter(0, 6000));
        TradeStationSearchCriteria.StationType stationType = new TradeStationSearchCriteria.StationType();
        stationType.setTypes(Arrays.asList("Asteroid base", "Coriolis Starport", "Mega ship", "Ocellus Starport", "Orbis Starport", "Planetary Port", "Space Station", "Odyssey Settlement", "Planetary Base", "outpost"));
        filters.setStationType(stationType);
        initialStationCriteria.setFilters(filters);

        /// NOTE: Spansh API is very inconsistent. We can't reuse RangeFilter because distance must be passed without "<=>"
        TradeStationSearchCriteria.Distance distance = new TradeStationSearchCriteria.Distance();
        distance.setMax(MAX_DISTANCE_TO_INITIAL_STATION);
        distance.setMin(0);
        filters.setDistanceToStarSystem(distance);

        initialStationCriteria.setFilters(filters);
        log.debug("Initial station criteria: {}", initialStationCriteria.toJson());

        TradeStationSearchResultDto startingStation = stationSearchClient.searchTradeStation(initialStationCriteria);


        if (startingStation.getResults().isEmpty()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("Could not find a suitable starting trade station."));
            return true;
        }
        // only one station should be returned if the station is null - the method will return false.
        if (startingStation.getResults().stream().anyMatch(this::isTooFar)) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("Nearest trade station is too far away to calculate a trade route."));
            return true;
        }
        criteria.setStation(startingStation.getResults().stream().findFirst().get().getName());
        criteria.setSystem(startingStation.getResults().stream().findFirst().get().getSystemName());
        return false;
    }

    private boolean isTooFar(TradeStationSearchResultDto.StationResult stationResult) {
        if (stationResult == null) return false;
        LocationDao.Coordinates myLocation = LocationManager.getInstance().getGalacticCoordinates();
        double distance = NavigationUtils.calculateGalacticDistance(
                stationResult.getSystemX(), stationResult.getSystemY(), stationResult.getSystemZ(),
                myLocation.x(), myLocation.y(), myLocation.z()
        );

        boolean isToFar = MAX_DISTANCE_TO_INITIAL_STATION < distance;
        if (isToFar) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("Found trade station within " + (int) distance + " light years."));
        }
        return isToFar;
    }

    private TradeProfileDao.TradeProfile getProfile(ShipDao.Ship ship) {
        if (ship == null) return null;
        TradeProfileDao.TradeProfile profile = Database.withDao(TradeProfileDao.class, dao -> {
            TradeProfileDao.TradeProfile p = dao.getTradeProfile(ship.getShipId());
            if (p == null) {
                p = new TradeProfileDao.TradeProfile();
                p.setShipId(ship.getShipId()); // unique id for this profile
                p.setPadSize(ShipPadSizes.getPadSize(ship.getShipIdentifier()));
                dao.save(p);
            }
            return p;
        });
        return profile;
    }


    public boolean setStartingCapitol(Integer startingCapital) {
        final ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("No ship data available. Please board a cargo ship."));
            return false;
        }
        TradeProfileDao.TradeProfile profile = getProfile(ship);
        profile.setStartingBudget(startingCapital);
        return Database.withDao(TradeProfileDao.class, dao -> {
            dao.save(profile);
            return true;
        });
    }

    public boolean setDistanceFromSystemEntry(Integer distance) {
        final ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("No ship data available. Please board a cargo ship."));
            return true;
        }
        TradeProfileDao.TradeProfile profile = getProfile(ship);
        profile.setMaxDistanceLs(distance);
        return Database.withDao(TradeProfileDao.class, dao -> {
            dao.save(profile);
            return true;
        });
    }


    public boolean setMaximumStops(Integer maxStops) {
        final ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("No ship data availale. Please board a cargo ship."));
            return false;
        }
        TradeProfileDao.TradeProfile profile = getProfile(ship);
        profile.setMaxJumps(maxStops);
        return Database.withDao(TradeProfileDao.class, dao -> {
            dao.save(profile);
            return true;
        });
    }

    public boolean setAllowFleetCarrier(boolean allowFleetCarrier) {
        final ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("No ship data availale. Please board a cargo ship."));
            return false;
        }
        TradeProfileDao.TradeProfile profile = getProfile(ship);
        profile.setAllowFleetCarrier(allowFleetCarrier);
        return Database.withDao(TradeProfileDao.class, dao -> {
            dao.save(profile);
            return true;
        });
    }

    public boolean setAllowPermit(boolean allowPermit) {
        final ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("No ship data available. Please board a cargo ship."));
            return false;
        }
        TradeProfileDao.TradeProfile profile = getProfile(ship);
        profile.setAllowPermit(allowPermit);
        return Database.withDao(TradeProfileDao.class, dao -> {
            dao.save(profile);
            return true;
        });
    }

    public boolean setAllowProhibitedCargo(boolean allowProhibitedCargo) {
        final ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("No ship data available. Please board a cargo ship."));
            return false;
        }
        TradeProfileDao.TradeProfile profile = getProfile(ship);
        profile.setAllowProhibited(allowProhibitedCargo);
        return Database.withDao(TradeProfileDao.class, dao -> {
            dao.save(profile);
            return true;
        });
    }


    public boolean setAllowPlanetaryPorts(boolean allowPlanetaryPorts) {
        final ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("No ship data availale. Please board a cargo ship."));
            return false;
        }
        TradeProfileDao.TradeProfile profile = getProfile(ship);
        profile.setAllowPlanetary(allowPlanetaryPorts);
        return Database.withDao(TradeProfileDao.class, dao -> {
            dao.save(profile);
            return true;
        });
    }


    public boolean hasCargoCapacity() {
        return shipManager.getShip() == null ? false : shipManager.getShip().getCargoCapacity() > 0;
    }

    public void setAllowStrongHolds(boolean isOn) {
        Database.withDao(TradeProfileDao.class, dao -> {
            final ShipDao.Ship ship = shipManager.getShip();
            TradeProfileDao.TradeProfile profile = getProfile(ship);
            profile.setAllowStrongHold(isOn);
            dao.save(profile);
            return Void.class;
        });
    }

    /**
     * Returns the trade profile for the given ship ID, creating a default one if absent.
     * Intended for UI use where the ship object is not available but the ID is known.
     */
    public TradeProfileDao.TradeProfile getOrCreateProfile(int shipId) {
        return Database.withDao(TradeProfileDao.class, dao -> {
            TradeProfileDao.TradeProfile p = dao.getTradeProfile(shipId);
            if (p == null) {
                p = new TradeProfileDao.TradeProfile();
                p.setShipId(shipId);
                dao.save(p);
            }
            return p;
        });
    }

    /** Persists the given trade profile to the database. */
    public void saveProfile(TradeProfileDao.TradeProfile profile) {
        Database.withDao(TradeProfileDao.class, dao -> {
            dao.save(profile);
            return Void.class;
        });
    }
}
