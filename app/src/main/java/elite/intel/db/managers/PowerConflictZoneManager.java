package elite.intel.db.managers;

import com.google.gson.reflect.TypeToken;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.dao.PowerConflictZoneDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.signals.PowerWarZone;
import elite.intel.util.json.GsonFactory;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the powers are fighting, learned from the commander's own flying and from the EDDN relay of
 * everyone else's.
 * <p>
 * The powerplay twin of {@link ConflictZoneManager}. A system enters the ledger when its power
 * conflict zones announce themselves on an FSS sweep, or when an arrival names the powers present,
 * and is offered only to a commander whose own power is one of them. Nothing here is ever announced.
 * <p>
 * WHY sightings expire: the powerplay cycle turns over weekly, so a zone not re-sighted in
 * {@link #ZONE_LIFETIME} is not offered, and a fresh sweep landing on such a row replaces its count
 * rather than max-merging with it.
 */
public class PowerConflictZoneManager {

    static final Duration ZONE_LIFETIME = Duration.ofDays(7);

    /**
     * How many results a search returns at most. The answer is spoken.
     */
    private static final int MAX_RESULTS = 5;

    private static final Type POWER_NAMES = new TypeToken<List<String>>() {
    }.getType();

    private static volatile PowerConflictZoneManager instance;

    private PowerConflictZoneManager() {
    }

    public static PowerConflictZoneManager getInstance() {
        if (instance == null) {
            synchronized (PowerConflictZoneManager.class) {
                if (instance == null) {
                    instance = new PowerConflictZoneManager();
                }
            }
        }
        return instance;
    }

    // ---------------------------------------------------------------- reads

    /**
     * Systems within {@code rangeLy} of {@code from} where {@code power} is in a power conflict zone
     * sighted within {@link #ZONE_LIFETIME}: the most recently sighted first, then the most zones,
     * then the nearest. A system whose powers no arrival has named yet is not offered, because
     * nothing says the commander's power is there.
     */
    public List<PowerWarZone> bestZonesFor(String power, Coordinates from, int rangeLy) {
        return bestZonesFor(power, from, rangeLy, Instant.now());
    }

    List<PowerWarZone> bestZonesFor(String power, Coordinates from, int rangeLy, Instant now) {
        if (from == null || power == null || power.isBlank()) return List.of();

        String seenSince = now.minus(ZONE_LIFETIME).toString();
        String quotedPower = '"' + power + '"';
        List<PowerConflictZoneDao.Nearby> rows = Database.withDao(PowerConflictZoneDao.class, dao ->
                dao.findNearby(from.x(), from.y(), from.z(), (double) rangeLy * rangeLy, seenSince, quotedPower, MAX_RESULTS));

        List<PowerWarZone> zones = new ArrayList<>();
        for (PowerConflictZoneDao.Nearby row : rows) {
            zones.add(new PowerWarZone(row.starSystem(), row.zones(), row.controllingPower(),
                    GsonFactory.getGson().fromJson(row.powers(), POWER_NAMES), Math.sqrt(row.distanceSq())));
        }
        return zones;
    }

    // --------------------------------------------------------------- writes

    /**
     * Records the power conflict zones one FSS sweep reported in a system. Safe to call for every
     * signal in the sweep as the count climbs.
     */
    public void recordZones(String starSystem, Long systemAddress, Coordinates coordinates, int zones, String seenAt) {
        if (starSystem == null || starSystem.isBlank() || systemAddress == null || zones <= 0 || seenAt == null) return;

        String staleBefore = staleBefore(seenAt);
        Database.withDao(PowerConflictZoneDao.class, dao -> {
            // The stale check reads lastSeen, so it must run before the sweep that advances it.
            if (staleBefore != null) dao.clearIfNotSeenSince(systemAddress, staleBefore);
            dao.recordSweep(systemAddress, starSystem, x(coordinates), y(coordinates), z(coordinates), zones, seenAt);
            return Void.TYPE;
        });
    }

    /**
     * Records which powers an arrival found in a system - or, when it found none, that the zones there
     * are gone.
     *
     * @param createIfMissing true for the commander's own arrivals, whose FSS sweep follows and needs the
     *                        row to join. False for the relay: an arrival somewhere with powers is most of
     *                        populated space at tens a second, and only the systems already known to hold
     *                        zones are worth a write
     */
    public void recordArrival(String starSystem,
                              Long systemAddress,
                              Coordinates coordinates,
                              String controllingPower,
                              List<String> powers,
                              String powerplayState,
                              String seenAt,
                              boolean createIfMissing) {
        if (starSystem == null || starSystem.isBlank() || systemAddress == null || seenAt == null) return;

        boolean noPowers = powers == null || powers.isEmpty();
        Database.withDao(PowerConflictZoneDao.class, dao -> {
            // WHY the read first: an UPDATE that matches nothing still takes the one WAL writer slot.
            if (!createIfMissing || noPowers) {
                if (!dao.exists(systemAddress)) return Void.TYPE;
            }
            if (noPowers) {
                dao.clearIfNotSeenAfter(systemAddress, seenAt);
            } else {
                dao.recordPowers(systemAddress, starSystem, x(coordinates), y(coordinates), z(coordinates),
                        controllingPower, GsonFactory.getGson().toJson(powers), powerplayState, seenAt);
            }
            return Void.TYPE;
        });
    }

    // -------------------------------------------------------------- helpers

    private static String staleBefore(String seenAt) {
        try {
            return Instant.parse(seenAt).minus(ZONE_LIFETIME).toString();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Double x(Coordinates c) {
        return c == null ? null : c.x();
    }

    private static Double y(Coordinates c) {
        return c == null ? null : c.y();
    }

    private static Double z(Coordinates c) {
        return c == null ? null : c.z();
    }
}
