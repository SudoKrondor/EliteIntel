package elite.intel.gameapi.search.spansh.starsystems;

import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.util.json.GsonFactory;

import java.util.ArrayList;
import java.util.List;

public class StarSystemClient extends SpanshClient {

    private static volatile StarSystemClient instance;

    private StarSystemClient() {
        super("https://spansh.co.uk/api/systems/search/save", "https://spansh.co.uk/api/systems/search/recall/");
    }

    public static StarSystemClient getInstance() {
        if (instance == null) {
            synchronized (StarSystemClient.class) {
                if (instance == null) {
                    instance = new StarSystemClient();
                }
            }
        }
        return instance;
    }

    public List<StationSearchResult.SystemResult> searchStarSystems(SystemSearchCriteria criteria) {
        StationSearchResult stationSearchResult = GsonFactory.getGson().fromJson(performSearch(criteria), StationSearchResult.class);
        if (stationSearchResult == null) return new ArrayList<>();
        List<StationSearchResult.SystemResult> results = stationSearchResult.getResults();
        return results != null ? results : new ArrayList<>();
    }
}
