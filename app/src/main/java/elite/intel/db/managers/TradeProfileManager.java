package elite.intel.db.managers;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.dao.TradeProfileDao;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.gameapi.search.spansh.station.StationSearchClient;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.session.PlayerSession;
import elite.intel.util.ShipPadSizes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TradeProfileManager {

    public static final int MAX_DISTANCE_TO_INITIAL_STATION = 500;
    /**
     * How far out of our own records a starting station may be picked before we ask Spansh instead.
     */
    public static final int MAX_DISTANCE_TO_KNOWN_STATION_LY = 20;
    /**
     * Spansh's own default hop distance, and the floor we hold the ship's jump range to.
     * <p>
     * A hop is a leg of the route and may take several jumps, so constraining it to the ship's one-jump
     * range only shrinks the search. Measured against the live planner from Daedalus in Sol with a 250t
     * hold: 18 Ly returned 16.1 million credits over four legs where 50 Ly returned 29.5 million.
     */
    public static final int DEFAULT_MAX_HOP_DISTANCE_LY = 50;
    /**
     * Journal station types with no large landing pad, so no route for a large ship can start at one.
     */
    private static final Set<String> NO_LARGE_PAD_STATION_TYPES = Set.of("Outpost", "CraterOutpost", "OnFootSettlement");
    /**
     * Journal station types that sit on a surface, gated by the profile's planetary flag.
     */
    private static final Set<String> SURFACE_STATION_TYPES = Set.of("CraterPort", "CraterOutpost", "OnFootSettlement");
    /**
     * Anchor candidates to ask Spansh for. More than one because the nearest station Spansh knows can still
     * be rejected here (a stale market flag, say), and the next one on the page is the next best answer.
     */
    private static final int ANCHOR_CANDIDATES = 20;
    /**
     * Arrival-distance cap for the anchor search while the profile has none set. The commander is told
     * about the missing setting by the calling command, so the anchor search stays permissive rather than
     * failing first with a less useful message.
     */
    private static final int ANCHOR_MAX_LS_WHEN_UNSET = 6000;
    /**
     * Comfortably above any station's large-pad count; the pad range is really "one or more".
     */
    private static final int LARGE_PADS_MANY = 100;
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
        criteria.setMaxJumpDistance(maxHopDistanceLy());
        criteria.setMaxLsFromArrival(profile.getMaxDistanceLs());
        criteria.setMaxJumps(profile.getMaxJumps());
        criteria.setAllowStrongHold(profile.isAllowStrongHold());
        criteria.setRequiresLargePad(shipManager.requireLargePad());
        criteria.setStartingCapital(profile.getStartingBudget());

        // Our own records first, Spansh second. Both helpers report true only once the criteria carries a
        // named station AND system, and the loser of the pair has already voiced why it came up empty.
        if (withStationStartingStation
                && !setStartingStationFromOurRecords(criteria)
                && !setStartingStationFromSpansh(criteria)) {
            return null;
        }
        log.debug("Trade route criteria: {}", criteria.toString());
        return criteria;
    }

    /**
     * The light years a single trade hop may span, floored at {@link #DEFAULT_MAX_HOP_DISTANCE_LY}.
     * <p>
     * WHY the loadout is read defensively: it stays absent until the game emits a Loadout event, and
     * dereferencing it in that window threw - taking every caller of {@code getCriteria} down with it,
     * including the ones that never wanted a starting station.
     */
    private int maxHopDistanceLy() {
        ShipLoadOutDto loadout = playerSession.getShipLoadout();
        int oneJump = loadout == null ? 0 : (int) loadout.getMaxJumpRange();
        return Math.max(oneJump, DEFAULT_MAX_HOP_DISTANCE_LY);
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
                .filter(station -> isNamed(station.getStationName()) && isNamed(station.getStarName()))
                .filter(TradeProfileManager::sellsCommodities)
                .filter(station -> canAnchorRoute(criteria, station))
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
     * Whether the commander can actually start a route at this station.
     * <p>
     * WHY we check rather than leave it to Spansh: the planner applies {@code requires_large_pad} and
     * {@code allow_planetary} to the route's DESTINATIONS only - a fixed source station is taken as given,
     * and a route plotted from a pad the ship cannot land on comes back looking perfectly valid. The
     * journal's station type is the only pad evidence we record; a type we do not recognise is taken as
     * usable, exactly as {@link #sellsCommodities} treats an unknown service list, because the older
     * station rows predate either being captured.
     */
    static boolean canAnchorRoute(TradeRouteSearchCriteria criteria, LocationDto station) {
        String stationType = station.getStationType();
        if (criteria.isRequiresLargePad() && NO_LARGE_PAD_STATION_TYPES.contains(stationType)) return false;
        return criteria.isAllowPlanetary() || !SURFACE_STATION_TYPES.contains(stationType);
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

    /**
     * Picks the nearest station Spansh knows that the route search will also accept as its anchor. Returns
     * false, having voiced the reason, when there is no such station or we do not know where we are.
     */
    private boolean setStartingStationFromSpansh(TradeRouteSearchCriteria criteria) {
        LocationDao.Coordinates galacticCoordinates = LocationManager.getInstance().getGalacticCoordinates();
        if (!hasKnownPosition(galacticCoordinates)) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("Galactic coordinates are not available."));
            return false;
        }

        TradeStationSearchCriteria anchorSearch = anchorSearchCriteria(criteria, galacticCoordinates);
        log.debug("Initial station criteria: {}", anchorSearch.toJson());
        TradeStationSearchResultDto response = StationSearchClient.getInstance().searchStations(anchorSearch);

        // A failed POST, a search that times out, and an empty body all arrive here as a null - Spansh
        // being unreachable is not a station we can plot from either way.
        List<TradeStationSearchResultDto.StationResult> results =
                response == null || response.getResults() == null ? List.of() : response.getResults();

        Optional<TradeStationSearchResultDto.StationResult> anchor = results.stream()
                .filter(station -> isNamed(station.getName()) && isNamed(station.getSystemName()))
                // Belt and braces over the Market service filter: only a station Spansh positively says has
                // no market is dropped, so a row that simply never recorded the flag still counts.
                .filter(station -> !Boolean.FALSE.equals(station.getHasMarket()))
                .findFirst();

        if (anchor.isEmpty()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("Unable to find a suitable initial trade station within " + MAX_DISTANCE_TO_INITIAL_STATION + " light years."));
            return false;
        }

        TradeStationSearchResultDto.StationResult station = anchor.get();
        criteria.setStation(station.getName());
        criteria.setSystem(station.getSystemName());
        log.debug("Starting trade station taken from Spansh: {} in {} ({} ly out)", station.getName(), station.getSystemName(), station.getDistance());
        announceAnchorDistance(station);
        return true;
    }

    /**
     * Sol really does sit at 0,0,0 - and so does a location we have recorded no coordinates for, because
     * the unset value and the real one are the same three zeroes. Only the star's name separates them.
     */
    private boolean hasKnownPosition(LocationDao.Coordinates galacticCoordinates) {
        if (galacticCoordinates == null) return false;
        boolean atOrigin = galacticCoordinates.x() == 0 && galacticCoordinates.y() == 0 && galacticCoordinates.z() == 0;
        return !atOrigin || "Sol".equalsIgnoreCase(playerSession.getPrimaryStarName());
    }

    /**
     * The anchor search request body.
     * <p>
     * WHY the market filter is not optional: asked to plot from a station with no commodity market, the
     * trade planner answers {@code Not Found} - there is nothing to buy, so there is no route. The pad and
     * surface filters are ours to apply for a different reason; see {@link #canAnchorRoute}.
     * <p>
     * WHY it is a separate, package-private method: the wire shape is the whole behaviour and can then be
     * asserted without a live call - Spansh ignores a filter key it does not recognise, and matches nothing
     * at all against a station type name it does not know, so either mistake narrows the search silently
     * instead of failing it.
     */
    static TradeStationSearchCriteria anchorSearchCriteria(TradeRouteSearchCriteria profile, LocationDao.Coordinates galacticCoordinates) {
        List<String> stationTypes = new ArrayList<>(TradeStationSearchCriteria.StationType.ORBITAL_TRADE_TYPES);
        if (profile.isAllowPlanetary())
            stationTypes.addAll(TradeStationSearchCriteria.StationType.PLANETARY_TRADE_TYPES);
        if (profile.isAllowFleetCarriers())
            stationTypes.addAll(TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES);
        TradeStationSearchCriteria.StationType stationType = new TradeStationSearchCriteria.StationType();
        stationType.setTypes(stationTypes);

        /// NOTE: Spansh API is very inconsistent. We can't reuse RangeFilter because distance must be passed without "<=>"
        TradeStationSearchCriteria.Distance distance = new TradeStationSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(MAX_DISTANCE_TO_INITIAL_STATION);

        int maxLsFromArrival = profile.getMaxLsFromArrival() > 0 ? profile.getMaxLsFromArrival() : ANCHOR_MAX_LS_WHEN_UNSET;

        TradeStationSearchCriteria.Filters filters = new TradeStationSearchCriteria.Filters();
        filters.setStationType(stationType);
        filters.setDistanceToStarSystem(distance);
        filters.setDistanceToArrival(new TradeStationSearchCriteria.RangeFilter(0, maxLsFromArrival));
        filters.setServices(List.of(new TradeStationSearchCriteria.Service(List.of(TradeStationSearchCriteria.MARKET_SERVICE))));
        if (profile.isRequiresLargePad()) {
            filters.setLargePads(new TradeStationSearchCriteria.RangeFilter(1, LARGE_PADS_MANY));
        }

        TradeStationSearchCriteria.ReferenceCoords coords = new TradeStationSearchCriteria.ReferenceCoords();
        coords.setX(galacticCoordinates.x());
        coords.setY(galacticCoordinates.y());
        coords.setZ(galacticCoordinates.z());

        TradeStationSearchCriteria anchorSearch = new TradeStationSearchCriteria();
        anchorSearch.setFilters(filters);
        anchorSearch.setReferenceCoords(coords);
        anchorSearch.setSort(List.of(new TradeStationSearchCriteria.DistanceSort()));
        anchorSearch.setSize(ANCHOR_CANDIDATES);
        anchorSearch.setPage(0);
        return anchorSearch;
    }

    /**
     * Says how far out the route begins when that is a journey rather than a hop, because the commander has
     * to fly there before the first leg starts.
     * <p>
     * Replaces a check that aborted the whole search whenever ANY station on the page was out of range -
     * which, on an unsorted page, threw away a perfectly good nearest station. Range is now the search's
     * job: Spansh never returns one past {@link #MAX_DISTANCE_TO_INITIAL_STATION}.
     */
    private void announceAnchorDistance(TradeStationSearchResultDto.StationResult station) {
        Double distance = station.getDistance();
        if (distance == null || distance <= MAX_DISTANCE_TO_KNOWN_STATION_LY) return;
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(
                "Nearest trade station is " + station.getName() + ", " + distance.intValue() + " light years out."
        ));
    }

    private static boolean isNamed(String value) {
        return value != null && !value.isBlank();
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
