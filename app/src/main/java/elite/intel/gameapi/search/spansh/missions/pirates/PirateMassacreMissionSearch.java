package elite.intel.gameapi.search.spansh.missions.pirates;

import elite.intel.db.dao.LocationDao;
import elite.intel.db.dao.PirateHuntingGroundsDao.HuntingGround;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.HuntingGroundManager.PirateMissionTuple;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.search.intra.IntraClient;
import elite.intel.gameapi.search.intra.IntraRequest;
import elite.intel.gameapi.search.intra.IntraResponse;
import elite.intel.gameapi.search.spansh.starsystems.StarSystemResult;
import elite.intel.gameapi.search.spansh.starsystems.StationClient;
import elite.intel.gameapi.search.spansh.starsystems.SystemSearchCriteria;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.List;

import static elite.intel.gameapi.search.edsm.utils.EnemyStarSystemFilter.isEnemyStrongholdPair;

public class PirateMassacreMissionSearch {

    private static volatile PirateMassacreMissionSearch instance;
    private final StationClient starSystemSearchClient = StationClient.getInstance();
    private final IntraClient missionSearchClient = IntraClient.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final HuntingGroundManager missionDataManager = HuntingGroundManager.getInstance();

    private PirateMassacreMissionSearch() {
    }

    public static PirateMassacreMissionSearch getInstance() {
        if (instance == null) {
            synchronized (PirateMassacreMissionSearch.class) {
                if (instance == null) instance = new PirateMassacreMissionSearch();
            }
        }
        return instance;
    }


    /**
     * Finds a list of hunting spots in range for pirate missions based on the given distance and reconnaissance mode.
     * The method uses the player's current galactic coordinates to search for missions within a specified range.
     * If no missions are found, it attempts to query an external pirate massacre mission API for additional results.
     *
     * @param range the maximum distance from the player's current coordinates to look for hunting spots
     * @return a list of {@code PirateMissionTuple} objects, where each tuple contains a {@code PirateFaction}
     *         representing the target faction and a list of {@code MissionProvider}s offering relevant missions
     */
    public List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> findHuntingSpotsInRange(int range) {
        LocationDao.Coordinates coordinates = locationManager.getGalacticCoordinates();
        return queryApi(coordinates, range);
    }


    /**
     * Queries an API to retrieve a list of pirate mission tuples based on the provided coordinates and range.
     * Filters and processes the data from the API to prepare mission pairs involving pirate factions
     * and their respective mission providers.
     *
     * @param coordinates the spatial coordinates in the galaxy used to locate potential pirate missions
     * @param range       the distance within which to search for pirate missions around the given coordinates
     * @return a list of pirate mission tuples, where each tuple contains a target pirate faction
     * and a list of corresponding mission providers; may return null if no valid data is retrieved
     */
    private List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> queryApi(LocationDao.Coordinates coordinates, int range) {
        String pledgedPower = playerSession.getRankAndProgressDto().getPledgedToPower();
        boolean pledgedToPower = (pledgedPower != null && !pledgedPower.isEmpty());

        IntraRequest request = new IntraRequest()
                .withResultLimit(40)
                .withMissionProviderMinFactions(1)
                .withArenaHazResDistanceFromEntryLs(50000)
                .withOurLocation(coordinates.x(), coordinates.y(), coordinates.z(), range);

        IntraResponse response = missionSearchClient.findMassacrePairs(request);
        if (response == null) return null;
        if (response.getOk() != 1) return null;
        if (response.getBody() == null) return null;
        if (response.getBody().isEmpty()) return null;

        List<IntraResponse.Pair> responseBody = response.getBody();
        List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> result = new ArrayList<>();

        for (IntraResponse.Pair pair : responseBody) {

            if (pair.getBettleGround() == null) continue;
            if (pair.getBettleGround().getHazRes() == null) continue;
            if (pair.getBettleGround().getHazRes().getConfirmed() < 1) continue;

            StarSystemResult missionProviderSystem = searchSystem(pair.getMissionProvider().getStarSystemName());
            StarSystemResult battleGroundSystem = searchSystem(pair.getBettleGround().getStarSystemName());

            if (missionProviderSystem == null || battleGroundSystem == null) continue;

            if (pledgedToPower && isEnemyStrongholdPair(missionProviderSystem, battleGroundSystem, pledgedPower))
                continue;

            result.add(
                    missionDataManager.savePartialPair(
                            missionProviderSystem.getRecord(), battleGroundSystem.getRecord()
                    )
            );
        }

        return result;
    }


    private StarSystemResult searchSystem(String starSystemName) {
        SystemSearchCriteria criteria = new SystemSearchCriteria();
        SystemSearchCriteria.Filters filters = new SystemSearchCriteria.Filters();
        SystemSearchCriteria.Distance distance = new SystemSearchCriteria.Distance();
        distance.setMax(100000);
        distance.setMin(0);
        SystemSearchCriteria.SystemNameFilter systemName = new SystemSearchCriteria.SystemNameFilter();
        systemName.setValue(starSystemName);
        filters.setSystemName(systemName);
        filters.setDistance(distance);
        criteria.setFilters(filters);
        criteria.setReferenceSystem(starSystemName);
        criteria.setPage(0);
        criteria.setSize(10);
        return starSystemSearchClient.search(criteria);
    }
}
