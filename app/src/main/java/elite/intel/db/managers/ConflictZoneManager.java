package elite.intel.db.managers;

import elite.intel.db.dao.ConflictZoneDao;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.util.Database;
import elite.intel.gameapi.signals.ConflictZoneProfile;
import elite.intel.gameapi.signals.WarZone;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the wars are, learned from the commander's own flying and from the EDDN relay of everyone
 * else's.
 * <p>
 * The twin of {@link HuntingGroundManager} for the other kind of fighting. A system becomes a war
 * zone because its conflict zones announced themselves on an FSS sweep, and gains its two sides
 * when an arrival's Conflicts block names them. Nothing here is ever announced: the ledger fills
 * up quietly and is read back when the commander asks for a fight.
 * <p>
 * WHY sightings expire: a resource extraction site is a feature of a ring and stays for ever, but
 * a war runs for days and then the zones are gone. A row unseen for {@link #WAR_LIFETIME} is not
 * offered, and a fresh sweep landing on such a row replaces its counts rather than max-merging
 * with them, so the last war never inflates this one.
 */
public class ConflictZoneManager {

    /**
     * How long a sighting is trusted. A faction war ends after seven days at the outside, so a zone
     * not re-sighted in that time is over.
     */
    static final Duration WAR_LIFETIME = Duration.ofDays(7);

    /**
     * How many results a search returns at most. The answer is spoken, and a commander cannot hold a
     * list longer than this in their head while flying.
     */
    private static final int MAX_RESULTS = 5;

    private static volatile ConflictZoneManager instance;

    private ConflictZoneManager() {
    }

    public static ConflictZoneManager getInstance() {
        if (instance == null) {
            synchronized (ConflictZoneManager.class) {
                if (instance == null) {
                    instance = new ConflictZoneManager();
                }
            }
        }
        return instance;
    }

    // ---------------------------------------------------------------- reads

    /**
     * War zones worth flying to within {@code rangeLy} of {@code from}, counting only sightings
     * still within {@link #WAR_LIFETIME}: the most recently sighted first, then the hardest
     * fighting, then the nearest.
     * <p>
     * WHY recency leads: the background simulation redraws the wars every week, so of two systems
     * in range the one someone saw fighting today is the one more likely to still be fighting when
     * the commander gets there. It is coarse on purpose - by day, not by minute - so that a low
     * zone sighted five minutes ago does not outrank a high one sighted an hour ago.
     */
    public List<WarZone> bestWarZones(Coordinates from, int rangeLy) {
        return bestWarZones(from, rangeLy, Instant.now());
    }

    List<WarZone> bestWarZones(Coordinates from, int rangeLy, Instant now) {
        if (from == null) return List.of();

        String seenSince = now.minus(WAR_LIFETIME).toString();
        List<ConflictZoneDao.Nearby> rows = Database.withDao(ConflictZoneDao.class, dao ->
                dao.findNearby(from.x(), from.y(), from.z(), squared(rangeLy), seenSince, MAX_RESULTS));

        List<WarZone> zones = new ArrayList<>();
        for (ConflictZoneDao.Nearby row : rows) {
            zones.add(new WarZone(
                    row.starSystem(),
                    new ConflictZoneProfile(row.low(), row.medium(), row.high(), row.powerplay()),
                    row.warType(),
                    row.faction1(),
                    row.faction2(),
                    Math.sqrt(row.distanceSq())
            ));
        }
        return zones;
    }

    /**
     * The conflict zones known in this system, or null when none are current - either because
     * there are none or because the last sighting is older than a war lasts.
     */
    public WarZone zonesIn(String starSystem) {
        return zonesIn(starSystem, Instant.now());
    }

    WarZone zonesIn(String starSystem, Instant now) {
        if (starSystem == null || starSystem.isBlank()) return null;
        ConflictZoneDao.Zone zone = Database.withDao(ConflictZoneDao.class, dao -> dao.findByName(starSystem));
        if (zone == null || zone.lastSeen() == null) return null;
        if (zone.lastSeen().compareTo(now.minus(WAR_LIFETIME).toString()) < 0) return null;
        ConflictZoneProfile profile = new ConflictZoneProfile(zone.low(), zone.medium(), zone.high(), zone.powerplay());
        if (profile.factionWarZones() == 0) return null;
        return new WarZone(zone.starSystem(), profile, zone.warType(), zone.faction1(), zone.faction2(), 0);
    }

    // --------------------------------------------------------------- writes

    /**
     * Records the conflict zones one FSS sweep reported in a system.
     * <p>
     * Safe to call for every signal in the sweep as the counts climb: within a war the counts only
     * ever rise, so an interrupted sweep leaves the system's best known inventory in place.
     */
    public void recordZones(String starSystem,
                            Long systemAddress,
                            Coordinates coordinates,
                            ConflictZoneProfile zones,
                            String seenAt) {
        if (starSystem == null || starSystem.isBlank() || zones == null || seenAt == null) return;

        String staleBefore = staleBefore(seenAt);
        Database.withDao(ConflictZoneDao.class, dao -> {
            // The stale check reads lastSeen, so it must run before the touch that advances it.
            if (staleBefore != null) dao.clearIfNotSeenSince(starSystem, staleBefore);
            dao.touch(starSystem, systemAddress, x(coordinates), y(coordinates), z(coordinates), seenAt);
            dao.recordSweep(starSystem, zones.low(), zones.medium(), zones.high(), zones.powerplay());
            return Void.TYPE;
        });
    }

    /**
     * Records who is fighting in a system, from an arrival's Conflicts block.
     * <p>
     * The row is touched so a war named before its zones are sighted has somewhere to put them,
     * but the counts stay at zero until a sweep says otherwise - the sides alone are not a place
     * to fly to.
     */
    public void recordWar(String starSystem,
                          Long systemAddress,
                          Coordinates coordinates,
                          String warType,
                          String faction1,
                          String faction2,
                          String seenAt) {
        if (starSystem == null || starSystem.isBlank() || seenAt == null) return;

        String staleBefore = staleBefore(seenAt);
        Database.withDao(ConflictZoneDao.class, dao -> {
            if (staleBefore != null) dao.clearIfNotSeenSince(starSystem, staleBefore);
            dao.touch(starSystem, systemAddress, x(coordinates), y(coordinates), z(coordinates), seenAt);
            dao.recordSides(starSystem, warType, faction1, faction2);
            return Void.TYPE;
        });
    }

    /**
     * Records that an arrival found no war in a system: the zones are gone, whatever the last sweep
     * said. Nothing happens for a system never recorded, so this is safe to call on every arrival.
     */
    public void recordPeace(String starSystem, String seenAt) {
        if (starSystem == null || starSystem.isBlank() || seenAt == null) return;
        Database.withDao(ConflictZoneDao.class, dao -> {
            // Anything seen up to and including this moment is over, so the cut-off sits just past it.
            dao.clearIfNotSeenSince(starSystem, seenAt + "~");
            return Void.TYPE;
        });
    }

    // -------------------------------------------------------------- helpers

    /**
     * The moment before which a sighting is too old to belong to the war being recorded now, or
     * null when the timestamp cannot be read - then nothing is cleared, which errs on the side of
     * keeping what is known.
     */
    private static String staleBefore(String seenAt) {
        try {
            return Instant.parse(seenAt).minus(WAR_LIFETIME).toString();
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

    private static double squared(int rangeLy) {
        return (double) rangeLy * rangeLy;
    }
}
