package elite.intel.search.spansh.starsystems;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.search.spansh.client.SpanshClient;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class StationClient extends SpanshClient {

    private final Logger log = LogManager.getLogger(StationClient.class);
    private static volatile StationClient instance;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = GsonFactory.getGson();

    public StationClient() {
        super("https://spansh.co.uk/api/systems/search/save", "https://spansh.co.uk/api/systems/search/recall/");
    }

    public static StationClient getInstance() {
        if (instance == null) {
            synchronized (StationClient.class) {
                if (instance == null) {
                    instance = new StationClient();
                }
            }
        }
        return instance;
    }


    public StarSystemResult search(SystemSearchCriteria criteria) {
        // search will return stations.
        // we have to make a GET call to https://spansh.co.uk/api/system/2209500629347 where the number is
        // system_id64
        StationSearchResult stationSearchResult = GsonFactory.getGson().fromJson(performSearch(criteria), StationSearchResult.class);
        if (stationSearchResult == null) return null;
        List<StationSearchResult.SystemResult> results = stationSearchResult.getResults();
        if (results == null) return null;
        for (StationSearchResult.SystemResult result : results) {
            if (result.getId64() == 0) continue;
            HttpRequest post = HttpRequest.newBuilder()
                    .uri(URI.create("https://spansh.co.uk/api/system/" + result.getId64()))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:144.0) Gecko/20100101 Firefox/144.0")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .GET()
                    .build();


            try {
                HttpResponse<String> resp = httpClient.send(post, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 400) {
                    log.warn("POST failed: {}", resp.body());
                    GameEventBus.publish(new SensorDataEvent("Unable to complete Spansh request: " + resp.body(), "Issue a warning"));
                }

                if (resp.statusCode() == 200) {
                    JsonObject json = gson.fromJson(resp.body(), JsonObject.class);
                    StarSystemResult starSystem = GsonFactory.getGson().fromJson(json, StarSystemResult.class);
                    String expectedName = (criteria.getFilters() != null && criteria.getFilters().getSystemName() != null)
                            ? criteria.getFilters().getSystemName().getValue()
                            : criteria.getReferenceSystem();
                    if (starSystem.getRecord() != null && expectedName.equalsIgnoreCase(starSystem.getRecord().getName())) {
                        return starSystem;
                    }
                }
            } catch (IOException | InterruptedException e) {
                // keep going
            }
        }
        return null;
    }
}
