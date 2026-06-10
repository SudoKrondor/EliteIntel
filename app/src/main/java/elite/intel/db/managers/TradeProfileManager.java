package elite.intel.db.managers;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.dao.TradeProfileDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.spansh.station.StationSearchClient;
import elite.intel.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.session.PlayerSession;
import elite.intel.util.NavigationUtils;
import elite.intel.util.ShipPadSizes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class TradeProfileManager {

    public static final int MAX_DISTANCE_TO_INITIAL_STATION = 500;
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
        criteria.setMaxJumpDistance(((int) playerSession.getShipLoadout().getMaxJumpRange() * 10));
        criteria.setMaxLsFromArrival(profile.getMaxDistanceLs());
        criteria.setMaxJumps(profile.getMaxJumps());
        criteria.setAllowStrongHold(profile.isAllowStrongHold());
        criteria.setRequiresLargePad(shipManager.requireLargePad());
        criteria.setStartingCapital(profile.getStartingBudget());

        if (withStationStartingStation) {
            List<LocationDto> stations = LocationManager.getInstance().findStationsInCurrentStarSystem(
                    playerSession.getLocationData().getSystemAddress()
            );
            if (stations.isEmpty()) {
                if (spanshSearchForStation(criteria)) return null;
            } else {
                LocationDto locationDto = stations.stream().findFirst().get();
                criteria.setStation(locationDto.getStationName());
                criteria.setSystem(locationDto.getStarName());
            }

            if (criteria.getStation() == null || criteria.getSystem() == null) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent("Unable to find a suitable initial trade station withing " + MAX_DISTANCE_TO_INITIAL_STATION + " light years."));
                return null;
            }
        }
        log.debug("Trade route criteria: {}", criteria.toString());
        return criteria;
    }

    private boolean spanshSearchForStation(TradeRouteSearchCriteria criteria) {
        StationSearchClient stationSearchClient = StationSearchClient.getInstance();

        TradeStationSearchCriteria initialStationCriteria = new TradeStationSearchCriteria();
        TradeStationSearchCriteria.ReferenceCoords coords = new TradeStationSearchCriteria.ReferenceCoords();
        LocationManager locationManager = LocationManager.getInstance();
        LocationDao.Coordinates galacticCoordinates = locationManager.getGalacticCoordinates();
        String primaryStarName = playerSession.getPrimaryStarName();

        if (galacticCoordinates == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("Galactic coordinates are not available."));
            return true;
        }

        /// Sol is 0,0,0 but also if we do not have coordinates, our location will be 0,0,0.
        if (galacticCoordinates.x() == 0 && galacticCoordinates.y() == 0 && galacticCoordinates.z() == 0 && !"Sol".equalsIgnoreCase(primaryStarName)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("Galactic coordinates are not available."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("Could not find a suitable starting trade station."));
            return true;
        }
        // only one station should be returned if the station is null - the method will return false.
        if (startingStation.getResults().stream().anyMatch(this::isTooFar)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("Nearest trade station is too far away to calculate a trade route."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("Found trade station within " + (int) distance + " light years."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("No ship data available. Please board a cargo ship."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("No ship data available. Please board a cargo ship."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("No ship data availale. Please board a cargo ship."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("No ship data availale. Please board a cargo ship."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("No ship data available. Please board a cargo ship."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("No ship data available. Please board a cargo ship."));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("No ship data availale. Please board a cargo ship."));
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
