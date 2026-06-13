package elite.intel.search.spansh.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.util.AudioPlayer;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;
import elite.intel.ws.WebSocketBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SpanshClient {

    private final Logger log = LogManager.getLogger(SpanshClient.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = GsonFactory.getGson();

    private String BASE_URL;
    private String RESULTS_URL;

    public SpanshClient(String BASE_URL, String RESULTS_URL) {
        this.BASE_URL = BASE_URL;
        this.RESULTS_URL = RESULTS_URL;
    }


    private String postSearch(ToJsonConvertible criteria) throws IOException, InterruptedException {
        return post(criteria.toJson());
    }

    private String postSearch(String criteria) throws IOException, InterruptedException {
        return post(criteria);
    }


    private String post(String criteria) throws IOException, InterruptedException {
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:144.0) Gecko/20100101 Firefox/144.0")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(criteria))
                .build();
        WebSocketBroadcaster.getInstance().broadcast(post);
        HttpResponse<String> resp = httpClient.send(post, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            log.warn("POST failed: {}", resp.statusCode());
            return null;
        }
        WebSocketBroadcaster.getInstance().broadcast(resp.body());
        JsonObject json = gson.fromJson(resp.body(), JsonObject.class);
        return json.has("search_reference") ? json.get("search_reference").getAsString() : null;
    }

    /**
     * Corner case, string query post, used by trade route plotter
     *
     */
    private String getSearch(String query) throws IOException, InterruptedException {

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:144.0) Gecko/20100101 Firefox/144.0")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", "https://spansh.co.uk")
                .header("Referer", "https://spansh.co.uk/trade")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .POST(HttpRequest.BodyPublishers.ofString(query))
                .build();

        WebSocketBroadcaster.getInstance().broadcast(post);
        HttpResponse<String> resp = httpClient.send(post, HttpResponse.BodyHandlers.ofString());
        String body = resp.body();
        if (resp.statusCode() == 400) {
            log.warn("POST failed: {}", body);
            EventBusManager.publish(new SensorDataEvent(
                            "Unable to complete search request. Spansh.co.uk failed with error message: " + body,
                            """
                                    Issue a warning with exact error message returned from API, let user know that Spansh failed to give us any data.
                                    Make that point clear. 
                                    Format your response for Speech to Text
                                    """
                    )
            );
        }

        if (resp.statusCode() != 202) {
            log.warn("POST failed: {}", resp.statusCode());
            return null;
        }

        WebSocketBroadcaster.getInstance().broadcast(body);
        JsonObject json = gson.fromJson(body, JsonObject.class);
        return json.has("job") ? json.get("job").getAsString() : null;
    }

    protected JsonObject performSearch(StringQuery query) {
        try {
            log.info("performing search: {}", BASE_URL + query.getQuery());
            String searchRefId = getSearch(query.getQuery());
            return waitForResults(searchRefId);
        } catch (IOException | InterruptedException e) {
            log.error("Error performing search", e);
        }
        return new JsonObject();
    }

    protected JsonObject performSearch(ToJsonConvertible criteria) {
        try {
            String searchRefId = postSearch(criteria);
            return waitForResults(searchRefId);
        } catch (IOException | InterruptedException e) {
            log.error("Error performing search", e);
        }
        return new JsonObject();
    }

    protected JsonObject performSearch(String json) {
        try {
            String searchRefId = postSearch(json);
            return waitForResults(searchRefId);
        } catch (IOException | InterruptedException e) {
            log.error("Error performing search", e);
        }
        return new JsonObject();
    }


    private JsonObject waitForResults(String searchRefId) throws IOException, InterruptedException {
        if (searchRefId == null) return null;
        WebSocketBroadcaster.getInstance().broadcast(searchRefId);
        int attempt = 0;
        final int maxAttempts = 60;
        long delay = 4_000L;

        while (attempt < maxAttempts) {
            attempt++;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(RESULTS_URL + searchRefId))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:144.0) Gecko/20100101 Firefox/144.0")
                    .GET()
                    .build();

            log.info("polling search {} (attempt {}) url: {}", searchRefId, attempt, req.uri());
            AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_3); // audio indicator of background search

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 202 || resp.statusCode() == 204) {
                // still processing
                delay = delay + (long) (Math.random() * 1_000);     // +0–1s jitter
                log.debug("Search {} in progress ({}), retry in {} ms. \n\n Body {}", searchRefId, resp.statusCode(), delay, resp.body());
                Thread.sleep(delay);
                continue;
            }

            if (resp.statusCode() == 200) {
                String body = resp.body().trim();
                if (body.isEmpty() || body.equals("null")) {
                    // known Spansh quirk – empty 200
                    Thread.sleep(2_000);
                    continue;
                }
                log.info("Search {} completed after {} attempts", searchRefId, attempt);
                WebSocketBroadcaster.getInstance().broadcast(body);
                return gson.fromJson(body, JsonObject.class);
            }

            log.warn("Unexpected status {} for search {}", resp.statusCode(), searchRefId);
            break;
        }

        log.error("Search {} timed out after {} attempts", searchRefId, maxAttempts);
        return null;
    }
}
