package elite.intel.gameapi.search.spansh.braintrees;

import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.util.json.GsonFactory;

public class BrainTreeClient extends SpanshClient {

    private static final String BASE_URL = "https://spansh.co.uk/api/bodies/search/save";
    private static final String RESULTS_URL = "https://spansh.co.uk/api/bodies/search/recall/";

    private static volatile BrainTreeClient instance;

    private BrainTreeClient() {
        super(BASE_URL, RESULTS_URL);
    }
    
    public static BrainTreeClient getInstance() {
        if (instance == null) {
            synchronized (BrainTreeClient.class) {
                if (instance == null) {
                    instance = new BrainTreeClient();
                }
            }
        }
        return instance;
    }

    public StellarObjectSearchResultDto search(BrainTreeCriteria criteria) {
        return GsonFactory.getGson().fromJson(performSearch(criteria), StellarObjectSearchResultDto.class);
    }
}
