package elite.intel.db.managers;

import elite.intel.db.dao.LocationDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.DockedMarket;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.util.NavigationUtils;
import elite.intel.util.json.GsonFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Singleton class for managing locations.
 * Use getInstance() to access the single instance of this class.
 */
public class LocationManager {
    private static LocationManager instance;

    /**
     * Striped locks that serialize read-modify-write of a single body across subscribers.
     * Journal events for the same body (e.g. SAASignalsFound + Scan) fire on separate virtual
     * threads within the same instant; without this they race and the later writer overwrites the
     * earlier writer's fields (blind whole-JSON upsert). See {@link #updateBody}.
     */
    private static final int WRITE_LOCK_STRIPES = 64;
    private final Object[] writeLocks = new Object[WRITE_LOCK_STRIPES];

    private LocationManager() {
        for (int i = 0; i < writeLocks.length; i++) {
            writeLocks[i] = new Object();
        }
    }

    public static synchronized LocationManager getInstance() {
        if (instance == null) {
            instance = new LocationManager();
        }
        return instance;
    }

    private Object lockFor(long systemAddress, long bodyId) {
        int hash = Long.hashCode(systemAddress * 31 + bodyId);
        return writeLocks[Math.floorMod(hash, WRITE_LOCK_STRIPES)];
    }

    /**
     * Atomically read-modify-write the body identified by {@code (systemAddress, bodyId)}: reads the
     * current record, applies {@code mutator}, and persists it, all under a per-body lock so a
     * concurrent {@code updateBody} for the same body cannot interleave and clobber fields. Callers
     * that must not lose fields to a racing subscriber should go through this instead of a bare
     * find/save pair.
     */
    public void updateBody(long systemAddress, long bodyId, Consumer<LocationDto> mutator) {
        synchronized (lockFor(systemAddress, bodyId)) {
            LocationDto location = findBySystemAddress(systemAddress, bodyId);
            mutator.accept(location);
            save(location);
        }
    }

    /**
     * Atomically read-modify-write the record stored under {@code locationName} - the table's own unique key,
     * and the one {@link #save} upserts on - while holding the body's write lock.
     * <p>
     * WHY by name rather than by BodyID: docking stores a station under the BodyID of the body we dropped at,
     * so a system with a station (or a fleet carrier parked at a body) has two records answering to the same
     * BodyID, and an ID lookup can hand back the wrong one. A caller that knows the place's name - the journal
     * always reports it - can address exactly the record its save will land on.
     * <p>
     * A record found under that name but belonging to another system is not adopted: station names are unique
     * per system, not across the galaxy, and merging a namesake's gravity and materials into this one would be
     * worse than starting a fresh record.
     */
    public void updateNamedBody(long systemAddress, long bodyId, String locationName, Consumer<LocationDto> mutator) {
        synchronized (lockFor(systemAddress, bodyId)) {
            LocationDto stored = findByLocationName(locationName);
            boolean sameSystem = stored != null && (stored.getSystemAddress() == 0 || stored.getSystemAddress() == systemAddress);
            LocationDto location = sameSystem ? stored : new LocationDto(bodyId, systemAddress);
            mutator.accept(location);
            save(location);
        }
    }

    private LocationDto findByLocationName(String locationName) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location location = dao.findByLocationName(locationName);
            return location == null ? null : GsonFactory.getGson().fromJson(location.getJson(), LocationDto.class);
        });
    }

    public void save(LocationDto location) {
        if (location.getStarName() == null) return;

        // WHY blank rather than null: planetName starts out as "" and setPlanetName(null) is a no-op, so a
        // station's record never had a null planet name to fall through on. Every one of them was written
        // under the empty name instead, and locationName is the table's unique key - so they all landed on
        // one row, each station overwriting the last. A record with no name at all is not stored: there is
        // nothing to find it by, and the row it used to occupy belonged to whatever was saved before it.
        String locationName = hasText(location.getPlanetName()) ? location.getPlanetName() : location.getStationName();
        if (!hasText(locationName)) return;

        Database.withDao(LocationDao.class, dao -> {
            dao.upsert(location.getBodyId(), locationName, location.getStarName(), location.getSystemAddress(), location.toJson());
            return null;
        });
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public LocationDao.Coordinates getGalacticCoordinates() {
        return Database.withDao(LocationDao.class, LocationDao::currentCoordinates);
    }


    public LocationDto getLocation(String primaryStar, Long locationId) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location entity = dao.findByInGameIdAndPrimaryStar(locationId, primaryStar);
            if (entity == null) {
                return new LocationDto(locationId, primaryStar);
            }
            return GsonFactory.getGson().fromJson(entity.getJson(), LocationDto.class);
        });
    }

    public LocationDto findBySystemAddress(long systemAddress, String planetName) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location location = dao.findPrimaryBySystemAddress(systemAddress, planetName);
            return location == null ? new LocationDto(-1L, systemAddress) : GsonFactory.getGson().fromJson(location.getJson(), LocationDto.class);
        });
    }

    public LocationDto findBySystemAddress(long systemAddress) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location location = dao.findPrimaryBySystemAddress(systemAddress);
            return location == null ? new LocationDto(-1L, systemAddress) : GsonFactory.getGson().fromJson(location.getJson(), LocationDto.class);
        });
    }


    public LocationDto findBySystemAddress(Long systemAddress, Long bodyId) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location location = dao.findPrimaryBySystemAddress(systemAddress, bodyId);
            return location == null ? new LocationDto(bodyId, systemAddress) : GsonFactory.getGson().fromJson(location.getJson(), LocationDto.class);
        });
    }

    /**
     * The record of the port the ship is standing on, or the current location when it is not on one.
     * <p>
     * Not the same question as "where am I": a fleet carrier, an orbital construction depot and a planetary
     * port are not the body the drop left us at, and the current location is that body. {@link DockedMarket}
     * holds the MarketID the journal gave on arrival - the one thing that names the pad unambiguously - and
     * the station is filed under it. The fallback is the answer callers used to get, for when we are in open
     * space or the station has no record yet.
     */
    public LocationDto findCurrentStation() {
        long marketId = DockedMarket.getInstance().marketId();
        if (marketId != 0) {
            LocationDto station = findByMarketId(marketId);
            if (station.getSystemAddress() > 0) return station;
        }
        return findByLocationData(PlayerSession.getInstance().getLocationData());
    }

    public LocationDto findByLocationData(LocationData<Long, Long> locationData) {
        return findBySystemAddress(locationData.getSystemAddress(), locationData.getInGameId());
    }


    public Map<Long, LocationDto> findByPrimaryStar(String primaryStar) {
        return Database.withDao(LocationDao.class, dao -> {
            List<LocationDao.Location> byPrimaryStar = dao.findByPrimaryStar(primaryStar);
            Map<Long, LocationDto> result = new HashMap<>();
            for (LocationDao.Location entity : byPrimaryStar) {
                result.put(entity.getInGameId(), GsonFactory.getGson().fromJson(entity.getJson(), LocationDto.class));
            }
            return result;
        });
    }

    public LocationDto findPrimaryStar(String starSystem) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location primaryStar = dao.findPrimaryStar(starSystem);
            return primaryStar == null ? new LocationDto(-1L) : GsonFactory.getGson().fromJson(primaryStar.getJson(), LocationDto.class);
        });
    }


    public LocationDto findByMarketId(long marketID) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location location = dao.findByMarketId(marketID);
            return location == null ? new LocationDto(-1L, -1L) : GsonFactory.getGson().fromJson(location.getJson(), LocationDto.class);
        });
    }

    public Collection<LocationDto> findAllBySystemAddress(long systemAddress) {
        return Database.withDao(LocationDao.class, dao -> {
            List<LocationDao.Location> bySystemAddress = dao.findAllBySystemAddress(systemAddress);
            Map<Long, LocationDto> result = new HashMap<>();
            for (LocationDao.Location entity : bySystemAddress) {
                result.put(entity.getInGameId(), GsonFactory.getGson().fromJson(entity.getJson(), LocationDto.class));
            }
            return result.values();
        });
    }

    /**
     * Bodies in one system that a surface scan found biological genuses on, in body order.
     * <p>
     * Filtered in SQL rather than by loading the system and testing each body, because a
     * well-explored system holds scores of rows and the caller wants the handful with biology.
     */
    public List<LocationDto> findBioSignalBodies(long systemAddress) {
        return Database.withDao(LocationDao.class, dao -> dao.findBioSignalBodies(systemAddress).stream()
                .map(entity -> GsonFactory.getGson().fromJson(entity.getJson(), LocationDto.class))
                .sorted(Comparator.comparingLong(LocationDto::getBodyId))
                .collect(Collectors.toList()));
    }

    public List<LocationDto> findStationsInCurrentStarSystem(long systemAddress) {
        return Database.withDao(LocationDao.class, dao -> {
            List<LocationDao.Location> stations = dao.findStationsInCurrentStarSystem(systemAddress);
            return stations.stream().map(entity -> GsonFactory.getGson().fromJson(entity.getJson(), LocationDto.class)).collect(Collectors.toList());
        });
    }

    /**
     * Stations we have already visited within {@code maxLightYears} of the given point, nearest first.
     * <p>
     * The database narrows the search to a cube around the point; the corners of that cube - which are
     * further away than the radius - are trimmed here with the real galactic distance.
     */
    public List<LocationDto> findKnownStationsWithin(LocationDao.Coordinates origin, double maxLightYears) {
        if (origin == null) return List.of();
        return Database.withDao(LocationDao.class, dao -> dao.findStationsInCoordinateBox(
                        origin.x() - maxLightYears, origin.x() + maxLightYears,
                        origin.y() - maxLightYears, origin.y() + maxLightYears,
                        origin.z() - maxLightYears, origin.z() + maxLightYears
                ).stream()
                .map(row -> Map.entry(
                        row,
                        NavigationUtils.calculateGalacticDistance(row.x(), row.y(), row.z(), origin.x(), origin.y(), origin.z())
                ))
                .filter(entry -> entry.getValue() <= maxLightYears)
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> GsonFactory.getGson().fromJson(entry.getKey().json(), LocationDto.class))
                .collect(Collectors.toList()));
    }
}
