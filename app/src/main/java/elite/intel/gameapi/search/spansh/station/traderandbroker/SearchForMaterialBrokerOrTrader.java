package elite.intel.gameapi.search.spansh.station.traderandbroker;

import elite.intel.gameapi.search.spansh.station.StationSearchClient;
import elite.intel.util.json.ToJsonConvertible;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class SearchForMaterialBrokerOrTrader {

    private static final Logger log = LogManager.getLogger(SearchForMaterialBrokerOrTrader.class);

    public static List<TraderAndBrokerSearchDto.Result> findMaterialTrader(ToJsonConvertible searchCriteria) {

        try {
            StationSearchClient client = StationSearchClient.getInstance();
            TraderAndBrokerSearchDto dto = client.searchTradersOrBrokers(searchCriteria);
            if (dto == null) return null;
            return dto.getResults();
        } catch (Exception e) {
            log.error("Failed to find material trader", e);
        }
        return null;
    }
}
