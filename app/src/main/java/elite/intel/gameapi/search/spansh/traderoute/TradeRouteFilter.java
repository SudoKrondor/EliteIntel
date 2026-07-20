package elite.intel.gameapi.search.spansh.traderoute;

import elite.intel.gameapi.search.spansh.starsystems.StarSystemResult;
import elite.intel.gameapi.search.spansh.starsystems.StationClient;
import elite.intel.gameapi.search.spansh.starsystems.SystemSearchCriteria;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.List;

import static elite.intel.gameapi.search.edsm.utils.EnemyStarSystemFilter.isEnemyStrongholdPair;

public class TradeRouteFilter {

    private static TradeRouteFilter instance;

    private final StationClient client = StationClient.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private TradeRouteFilter() {
    }

    public static synchronized TradeRouteFilter getInstance() {
        if (instance == null) {
            instance = new TradeRouteFilter();
        }
        return instance;
    }

    public TradeRouteResponse filter(TradeRouteResponse route) {

        if (route == null) return null; // no filter needed
        String pledgedPower = playerSession.getRankAndProgressDto().getPledgedToPower();
        if (pledgedPower == null || pledgedPower.isEmpty()) return route; // no filter needed

        List<TradeRouteTransaction> originalResults = route.getResult();
        if (originalResults == null) return route;
        List<TradeRouteTransaction> filteredResults = new ArrayList<>();

        for (TradeRouteTransaction transaction : originalResults) {
            String ogSource = transaction.getSource().getSystem();
            String ogDestination = transaction.getDestination().getSystem();

            StarSystemResult ogSourceSystem = searchSystem(ogSource);
            StarSystemResult ogDestinationSystem = searchSystem(ogDestination);

            if (isEnemyStrongholdPair(ogSourceSystem, ogDestinationSystem, pledgedPower)) continue;

            filteredResults.add(transaction);
        }
        route.setResult(filteredResults);
        return route;
    }

    private StarSystemResult searchSystem(String starSystemName) {
        SystemSearchCriteria criteria = new SystemSearchCriteria();
        SystemSearchCriteria.Filters filters = new SystemSearchCriteria.Filters();
        SystemSearchCriteria.SystemNameFilter systemName = new SystemSearchCriteria.SystemNameFilter();
        systemName.setValue(starSystemName);
        filters.setSystemName(systemName);
        criteria.setFilters(filters);
        criteria.setPage(0);
        criteria.setSize(10);
        return client.search(criteria);
    }
}
