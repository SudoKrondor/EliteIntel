package elite.intel.gameapi.search.spansh.stellarobjects;

import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.util.json.GsonFactory;

public class StellarObjectSearchClient extends SpanshClient {

    private static StellarObjectSearchClient instance;

    private StellarObjectSearchClient() {
        super("https://spansh.co.uk/api/bodies/search/save", "https://spansh.co.uk/api/bodies/search/recall/");
    }

    public static synchronized StellarObjectSearchClient getInstance() {
        if (instance == null) {
            instance = new StellarObjectSearchClient();
        }
        return instance;
    }

    public StellarObjectSearchResultDto search(StellarObjectSearchRequestDto criteria) {
        return GsonFactory.getGson().fromJson(performSearch(criteria), StellarObjectSearchResultDto.class);
    }
}
