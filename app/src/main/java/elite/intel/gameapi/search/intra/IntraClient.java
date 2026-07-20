package elite.intel.gameapi.search.intra;

import com.google.gson.Gson;
import elite.intel.tools.ws.WebSocketBroadcaster;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpRequest.newBuilder;

public class IntraClient {

    private static final Logger log = LogManager.getLogger(IntraClient.class);

    private static final String ENDPOINT = "https://iniv.space/intra/api/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static volatile IntraClient instance;
    private final Gson gson = GsonFactory.getGson();

    private IntraClient() {
    }

    public static IntraClient getInstance() {
        if (instance == null) {
            synchronized (IntraClient.class) {
                if (instance == null) instance = new IntraClient();
            }
        }
        return instance;
    }

    public IntraResponse findMassacrePairs(IntraRequest request) {
        try {
            String json = gson.toJson(request);
            WebSocketBroadcaster.getInstance().broadcast(json);
            HttpRequest httpRequest = newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Failed to query Intra API. HTTP response code: {}", response.statusCode());
                return null;
            }

            String body = response.body();
            WebSocketBroadcaster.getInstance().broadcast(body);
            return gson.fromJson(body, IntraResponse.class);
        } catch (Exception e) {
            log.error("Failed to query Intra API", e);
            return null;
        }
    }
}
