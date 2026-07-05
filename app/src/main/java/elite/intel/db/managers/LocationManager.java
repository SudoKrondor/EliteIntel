package elite.intel.db.managers;

import elite.intel.db.dao.LocationDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.LocationData;
import elite.intel.util.json.GsonFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void save(LocationDto location) {
        if (location.getStarName() == null) return;
        Database.withDao(LocationDao.class, dao -> {
            String locationName = location.getPlanetName() == null ? location.getStationName() : location.getPlanetName();
            dao.upsert(location.getBodyId(), locationName, location.getStarName(), location.getSystemAddress(), location.toJson());
            return null;
        });
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

    public LocationDto findByStatus(GameEvents.StatusEvent.Destination destination) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location location = dao.findBySystemAddressAndInGameId(destination.getSystem(), destination.getBody());
            return location == null ? new LocationDto(-1L) : GsonFactory.getGson().fromJson(location.getJson(), LocationDto.class);
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

    public LocationDto findByLocationName(String type) {
        return Database.withDao(LocationDao.class, dao -> {
            LocationDao.Location location = dao.findByLocationName(type);
            return location == null ? null : GsonFactory.getGson().fromJson(location.getJson(), LocationDto.class);
        });
    }

    public List<LocationDto> findStationsInCurrentStarSystem(long systemAddress) {
        return Database.withDao(LocationDao.class, dao -> {
            List<LocationDao.Location> stations = dao.findStationsInCurrentStarSystem(systemAddress);
            return stations.stream().map(entity -> GsonFactory.getGson().fromJson(entity.getJson(), LocationDto.class)).collect(Collectors.toList());
        });
    }
}
