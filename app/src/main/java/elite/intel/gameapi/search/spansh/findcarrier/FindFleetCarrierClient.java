package elite.intel.gameapi.search.spansh.findcarrier;

import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.util.json.GsonFactory;

public class FindFleetCarrierClient extends SpanshClient {

    private static FindFleetCarrierClient instance;

    private FindFleetCarrierClient() {
        super("https://spansh.co.uk/api/stations/search/save", "https://spansh.co.uk/api/stations/search/recall/");
    }

    public static synchronized FindFleetCarrierClient getInstance() {
        if (instance == null) {
            instance = new FindFleetCarrierClient();
        }
        return instance;
    }

    public FleetCarrierSearchResultsDto search(FleetCarrierSearchCriteriaDto criteria) {
        return GsonFactory.getGson().fromJson(performSearch(criteria), FleetCarrierSearchResultsDto.class);
    }
}
