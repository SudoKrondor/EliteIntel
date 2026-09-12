package elite.intel.db.managers;

import elite.intel.db.dao.HuntingGroundDao;
import elite.intel.db.dao.HuntingGroundScanDao;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.dao.MassacreMissionDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.HuntingGround;
import elite.intel.gameapi.missions.MassacrePair;
import elite.intel.gameapi.missions.ResourceSiteProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What the commander has learned about where to hunt pirates, from their own flying.
 * <p>
 * Two facts are recorded separately and joined only when both are in hand. A system becomes a
 * hunting ground because its resource extraction sites announced themselves on arrival. A provider
 * becomes paired with one because a pirate massacre contract taken there sends the commander to it.
 * A contract against a system that has never shown resource sites is not a pair, and is left on
 * file unpaired until the commander flies there and finds out either way.
 * <p>
 * Nothing here calls a network. It replaced a crowd-sourced pairing service that went quiet.
 */
public class HuntingGroundManager {

    /**
     * How many results a search returns at most. The answer is spoken, and a commander cannot hold a
     * list longer than this in their head while flying.
     */
    private static final int MAX_RESULTS = 5;

    private static volatile HuntingGroundManager instance;

    private HuntingGroundManager() {
    }

    public static HuntingGroundManager getInstance() {
        if (instance == null) {
            synchronized (HuntingGroundManager.class) {
                if (instance == null) {
                    instance = new HuntingGroundManager();
                }
            }
        }
        return instance;
    }

    // ---------------------------------------------------------------- reads

    /**
     * Provider systems worth flying to, deepest stack first, within {@code rangeLy} of {@code from}.
     * <p>
     * Distance is measured to the provider system because that is where the commander flies first -
     * the hunting ground is reached from there, usually in one jump, and often it is the same system.
     */
    public List<MassacrePair> bestPairs(Coordinates from, int rangeLy) {
        return pairs(from, null, squared(rangeLy));
    }

    /**
     * Provider systems that issue contracts against one named hunting ground, deepest stack first
     * and at any distance - the commander asked about this hunting ground, not about what is nearby.
     */
    public List<MassacrePair> providersFor(String targetSystem, Coordinates from) {
        if (targetSystem == null || targetSystem.isBlank()) return List.of();
        return pairs(from, targetSystem, Double.MAX_VALUE);
    }

    private List<MassacrePair> pairs(Coordinates from, String targetSystem, double maxDistanceSq) {
        if (from == null) return List.of();

        List<MassacreMissionDao.Pair> rows = Database.withDao(MassacreMissionDao.class, dao ->
                dao.findPairs(from.x(), from.y(), from.z(), maxDistanceSq, targetSystem, MAX_RESULTS));

        List<MassacrePair> pairs = new ArrayList<>();
        for (MassacreMissionDao.Pair row : rows) {
            pairs.add(new MassacrePair(
                    row.providerSystem(),
                    split(row.providerFactions()),
                    split(row.stations()),
                    row.targetSystem(),
                    row.targetFaction(),
                    new ResourceSiteProfile(row.standard(), row.low(), row.high(), row.hazardous()),
                    row.missionsCompleted(),
                    Math.sqrt(row.distanceSq())
            ));
        }
        return pairs;
    }

    /**
     * Hunting grounds worth flying to for the bounties alone, best fighting first, within
     * {@code rangeLy} of {@code from}. No contract is involved and none is needed.
     */
    public List<HuntingGround> bestHuntingGrounds(Coordinates from, int rangeLy) {
        if (from == null) return List.of();

        List<HuntingGroundDao.Nearby> rows = Database.withDao(HuntingGroundDao.class, dao ->
                dao.findNearby(from.x(), from.y(), from.z(), squared(rangeLy), MAX_RESULTS));

        List<HuntingGround> grounds = new ArrayList<>();
        for (HuntingGroundDao.Nearby row : rows) {
            grounds.add(new HuntingGround(
                    row.starSystem(),
                    new ResourceSiteProfile(row.standard(), row.low(), row.high(), row.hazardous()),
                    Math.sqrt(row.distanceSq())
            ));
        }
        return grounds;
    }

    /**
     * The resource sites known in this system, or null when it is not a hunting ground - either
     * because it has none or because the commander has never been there.
     */
    public ResourceSiteProfile sitesIn(String starSystem) {
        if (starSystem == null || starSystem.isBlank()) return null;
        HuntingGroundDao.Ground ground = Database.withDao(HuntingGroundDao.class, dao -> dao.findByName(starSystem));
        if (ground == null || ground.forgotten()) return null;
        return new ResourceSiteProfile(ground.standard(), ground.low(), ground.high(), ground.hazardous());
    }

    /**
     * How much the app knows, for the journal scan to report when it finishes.
     */
    public Knowledge knowledge() {
        int grounds = Database.withDao(HuntingGroundDao.class, HuntingGroundDao::count);
        int contracts = Database.withDao(MassacreMissionDao.class, MassacreMissionDao::count);
        return new Knowledge(grounds, contracts);
    }

    /**
     * The journal file the last backfill scan read to the end, or null when none has run - so a re-run
     * reads only what is new.
     */
    public String lastScannedJournal() {
        return Database.withDao(HuntingGroundScanDao.class, HuntingGroundScanDao::lastJournal);
    }

    // ---------------------------------------------------------------- writes

    public void rememberScannedJournal(String journalFileName, String scannedAt) {
        Database.withDao(HuntingGroundScanDao.class, dao -> {
            dao.recordProgress(journalFileName, scannedAt);
            return Void.TYPE;
        });
    }

    /**
     * Records the resource sites one FSS sweep reported in a system.
     * <p>
     * Safe to call for every signal in the sweep as the counts climb: the grade counts only ever
     * rise, so an interrupted sweep leaves the system's best known inventory in place rather than
     * overwriting it with a partial one.
     */
    public void recordResourceSites(String starSystem,
                                    Long systemAddress,
                                    Coordinates coordinates,
                                    ResourceSiteProfile sites,
                                    String seenAt) {
        if (starSystem == null || starSystem.isBlank() || sites == null) return;

        Database.withDao(HuntingGroundDao.class, dao -> {
            dao.touch(starSystem, systemAddress,
                    coordinates == null ? null : coordinates.x(),
                    coordinates == null ? null : coordinates.y(),
                    coordinates == null ? null : coordinates.z(),
                    seenAt);
            dao.recordSweep(starSystem, sites.standard(), sites.low(), sites.high(), sites.hazardous());
            return Void.TYPE;
        });
    }

    /**
     * Records a pirate massacre contract and where it was taken.
     *
     * @return true when this contract was new, false when it was already on file - which is how a
     * re-run of the journal scan stays silent about work it has already learned
     */
    public boolean recordContract(MissionDto mission, Coordinates providerLocation, String providerStation) {
        if (mission == null || providerLocation == null) return false;
        if (mission.getDestinationSystem() == null || mission.getDestinationSystem().isBlank()) return false;

        return Database.withDao(MassacreMissionDao.class, dao -> dao.record(
                mission.getMissionId(),
                providerLocation.primaryStar(),
                providerLocation.x(),
                providerLocation.y(),
                providerLocation.z(),
                providerStation,
                mission.getFaction(),
                mission.getDestinationSystem(),
                mission.getMissionTargetFaction(),
                mission.getKillCount(),
                mission.getReward(),
                mission.getAcceptedAt()
        )) == 1;
    }

    public void recordContractCompleted(long missionId, String completedAt) {
        Database.withDao(MassacreMissionDao.class, dao -> dao.markCompleted(missionId, completedAt));
    }

    /**
     * Stops offering a hunting ground, and with it every pairing whose contracts point at it.
     * <p>
     * WHY the rows survive, the contracts included: the commander forgets a ground because it is poor -
     * thin spawns, small bounties, a ring an hour out from the star - and none of that is in the journal.
     * Deleting the ground would let the next arrival record the system all over again and the app would
     * recommend it straight back, and deleting its contracts would let the next journal scan re-insert
     * them - the ledger is keyed on the game's MissionID precisely so that re-reading is harmless. The
     * flag is the verdict: the pair search joins on it, so nothing under a forgotten ground is offered,
     * and the sightings and contracts underneath stay honest.
     */
    public ForgetResult forget(String starSystem) {
        if (starSystem == null || starSystem.isBlank()) return new ForgetResult(false, 0);

        boolean known = Database.withDao(HuntingGroundDao.class, dao -> dao.forget(starSystem)) > 0;
        int contracts = Database.withDao(MassacreMissionDao.class, dao -> dao.countForTarget(starSystem));
        return new ForgetResult(known, contracts);
    }

    // ---------------------------------------------------------------- helpers

    private static double squared(int rangeLy) {
        return (double) rangeLy * rangeLy;
    }

    /**
     * Splits what GROUP_CONCAT returned: one entry per contract, joined on a character no faction or
     * station name can carry, so a name with a comma in it (player minor factions are free text) stays one
     * name. Repeats are dropped here rather than in SQL, which cannot de-duplicate and pick the separator
     * at the same time.
     */
    private static List<String> split(String concatenated) {
        if (concatenated == null || concatenated.isBlank()) return List.of();
        return Arrays.stream(concatenated.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * @param wasKnown          false when the system was never a hunting ground, so there was
     *                          nothing to forget and the commander should be told so
     * @param contractsAgainstIt how many recorded contracts pointed at it, and so go unoffered with it
     */
    public record ForgetResult(boolean wasKnown, int contractsAgainstIt) {
    }

    public record Knowledge(int huntingGrounds, int contracts) {
    }
}
