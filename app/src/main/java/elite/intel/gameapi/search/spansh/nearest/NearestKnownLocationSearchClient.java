package elite.intel.gameapi.search.spansh.nearest;

import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public class NearestKnownLocationSearchClient {

    private static final Logger log = LogManager.getLogger(NearestKnownLocationSearchClient.class);
    private static final String SPANSH_API_URL = "https://spansh.co.uk/api/nearest";
    private static final String USER_AGENT = "EliteIntel/1.0 (https://github.com/elite-intel)";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Finds the nearest known star system to the given coordinates using the Spansh API.
     *
     * @param x The X coordinate in light-years.
     * @param y The Y coordinate in light-years.
     * @param z The Z coordinate in light-years.
     * @return A {@link LocationDto} with the nearest system's details, or null if the request fails
     *         (e.g., non-200 status code, invalid response, or missing required fields).
     */
    public static LocationDto findNearest(double x, double y, double z) {
        try {
            String url = String.format(Locale.ROOT, "%s?x=%.15f&y=%.15f&z=%.15f", SPANSH_API_URL, x, y, z);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.warn("Failed to get nearest known location: status={}, response={}", response.statusCode(), response.body());
                return null;
            }

            SpanshSystemResponse spanshResponse = GsonFactory.getGson().fromJson(response.body(), SpanshSystemResponse.class);

            if (spanshResponse == null || spanshResponse.system == null
                    || spanshResponse.system.name == null
                    || spanshResponse.system.x == null
                    || spanshResponse.system.y == null
                    || spanshResponse.system.z == null
                    || spanshResponse.system.distance == null) {
                log.warn("Invalid or missing system data in response: {}", response.body());
                return null;
            }

            LocationDto temp = new LocationDto(-1L);
            temp.setLocationType(LocationDto.LocationType.STAR);
            temp.setStarName(spanshResponse.system.name);
            temp.setX(spanshResponse.system.x);
            temp.setY(spanshResponse.system.y);
            temp.setZ(spanshResponse.system.z);
            temp.setDistance(spanshResponse.system.distance);

            return temp;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to get nearest known location: {}", e.getMessage(), e);
            return null;
        }
    }

    private static class SpanshSystemResponse {
        private SystemData system;

        private static class SystemData {
            private String name;
            private Double x;
            private Double y;
            private Double z;
            private Double distance;
        }
    }
}