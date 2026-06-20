package elite.intel.search.edsm;

import com.google.gson.JsonSyntaxException;
import elite.intel.search.edsm.dto.*;
import elite.intel.search.edsm.dto.data.*;
import elite.intel.util.SleepNoThrow;
import elite.intel.util.json.GsonFactory;
import elite.intel.ws.WebSocketBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class EdsmApiClient {
    private static final Logger log = LogManager.getLogger(EdsmApiClient.class);
    private static final AtomicLong lastRequestTime = new AtomicLong(0L);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static String baseUrl() {
        return System.getProperty("edsm.base.url", "https://www.edsm.net");
    }

    private static long minIntervalMs() {
        return Long.getLong("edsm.min.interval.ms", 1_000L);
    }

    private static StringBuilder publicUrl(String endpoint) {
        return new StringBuilder(baseUrl() + endpoint + "?");
    }

    public static StarSystemDto searchStarSystem(String starSystemName, int showInformation) {
        if (starSystemName == null) return new StarSystemDto();
        String endpoint = "/api-v1/systems";
        StringBuilder query = publicUrl(endpoint);
        try {
            query.append("systemName=").append(URLEncoder.encode(starSystemName, StandardCharsets.UTF_8));
            query.append("&showInformation=").append(showInformation);
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return new StarSystemDto();
        }
        String response = callEdsm(query);
        long timestamp = System.currentTimeMillis();
        StarSystemData data;
        if (response.isEmpty()) {
            data = new StarSystemData();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, StarSystemData.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM: {}", response, e);
                return new StarSystemDto();
            }
        }
        StarSystemDto dto = new StarSystemDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static StarsWithinRadiusDto searchStarSystems(String starSystemName, int radius) {
        if (starSystemName == null) return new StarsWithinRadiusDto();
        String endpoint = "/api-v1/sphere-systems";
        StringBuilder query = publicUrl(endpoint);
        try {
            query.append("systemName=").append(URLEncoder.encode(starSystemName, StandardCharsets.UTF_8));
            query.append("&minRadius=1");
            query.append("&radius=").append(radius);
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return new StarsWithinRadiusDto();
        }
        String response = callEdsm(query);
        if (response.isEmpty()) {
            return new StarsWithinRadiusDto();
        }
        try {
            return GsonFactory.getGson().fromJson(response, StarsWithinRadiusDto.class);
        } catch (JsonSyntaxException e) {
            log.warn("Invalid JSON from EDSM: {}", response, e);
            return new StarsWithinRadiusDto();
        }
    }

    public static SystemBodiesDto searchSystemBodies(String starSystemName) {
        if (starSystemName == null) return new SystemBodiesDto();
        String endpoint = "/api-system-v1/bodies";
        StringBuilder query = publicUrl(endpoint);
        try {
            query.append("systemName=").append(URLEncoder.encode(starSystemName, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return new SystemBodiesDto();
        }
        String response = callEdsm(query);
        log.debug("EDSM bodies response for {}: {}", starSystemName, response);  // Swapped to debug
        long timestamp = System.currentTimeMillis();
        SystemBodiesData data;
        if (response.isEmpty()) {
            data = new SystemBodiesData();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, SystemBodiesData.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM bodies: {}", response, e);
                data = new SystemBodiesData();
            }
        }
        SystemBodiesDto dto = new SystemBodiesDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static EstimatedScanValuesDto searchEstimatedScanValues(String starSystemName) {
        if (starSystemName == null) return new EstimatedScanValuesDto();
        String endpoint = "/api-system-v1/estimated-value";
        StringBuilder query = publicUrl(endpoint);
        try {
            query.append("systemName=").append(URLEncoder.encode(starSystemName, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return new EstimatedScanValuesDto();
        }
        String response = callEdsm(query);
        long timestamp = System.currentTimeMillis();
        EstimatedScanValuesData data;
        if (response.isEmpty()) {
            data = new EstimatedScanValuesData();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, EstimatedScanValuesData.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM estimated-value: {}", response, e);
                data = new EstimatedScanValuesData();
            }
        }
        EstimatedScanValuesDto dto = new EstimatedScanValuesDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static StationsDto searchStations(String starSystemName, int sleep) {
        if (starSystemName == null) return new StationsDto();
        SleepNoThrow.sleep(sleep);

        String endpoint = "/api-system-v1/stations";
        StringBuilder query = publicUrl(endpoint);
        try {
            query.append("systemName=").append(URLEncoder.encode(starSystemName, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return new StationsDto();
        }
        String response = callEdsm(query);
        long timestamp = System.currentTimeMillis();
        StationsData data;
        if (response.isEmpty()) {
            data = new StationsData();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, StationsData.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM stations: {}", response, e);
                data = new StationsData();
            }
        }
        StationsDto dto = new StationsDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static FactionDto searchFaction(String starSystemName, int showHistory) {
        if (starSystemName == null) return new FactionDto();
        String endpoint = "/api-system-v1/factions";
        StringBuilder query = publicUrl(endpoint);
        try {
            query.append("systemName=").append(URLEncoder.encode(starSystemName, StandardCharsets.UTF_8));
            if (showHistory == 1) {
                query.append("&showHistory=1");
            }
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return new FactionDto();
        }
        String response = callEdsm(query);
        long timestamp = System.currentTimeMillis();
        FactionStats data;
        if (response.isEmpty()) {
            data = new FactionStats();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, FactionStats.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM factions: {}", response, e);
                data = new FactionStats();
            }
        }
        FactionDto dto = new FactionDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static TrafficDto searchTraffic(String starSystemName) {
        if (starSystemName == null) return new TrafficDto();
        String endpoint = "/api-system-v1/traffic";
        StringBuilder query = publicUrl(endpoint);
        try {
            query.append("systemName=").append(URLEncoder.encode(starSystemName, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return new TrafficDto();
        }
        String response = callEdsm(query);
        long timestamp = System.currentTimeMillis();
        TrafficData data;
        if (response.isEmpty()) {
            data = new TrafficData();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, TrafficData.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM traffic: {}", response, e);
                data = new TrafficData();
            }
        }
        TrafficDto dto = new TrafficDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static DeathsDto searchDeaths(String starSystemName) {
        if (starSystemName == null) return new DeathsDto();
        String endpoint = "/api-system-v1/deaths";
        StringBuilder query = publicUrl(endpoint);
        try {
            query.append("systemName=").append(URLEncoder.encode(starSystemName, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return new DeathsDto();
        }
        String response = callEdsm(query);
        long timestamp = System.currentTimeMillis();
        DeathsData data;
        if (response.isEmpty()) {
            data = new DeathsData();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, DeathsData.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM deaths: {}", response, e);
                data = new DeathsData();
            }
        }
        DeathsDto dto = new DeathsDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static MarketDto searchMarket(long marketId, String orSystemName, String andStationName, int sleep) {
        if (marketId <= 0 && (orSystemName == null || andStationName == null)) return new MarketDto();
        SleepNoThrow.sleep(sleep);
        String endpoint = "/api-system-v1/stations/market";
        String response = getServicesResponse(marketId, orSystemName, andStationName, endpoint);
        long timestamp = System.currentTimeMillis();
        MarketStats data;
        if (response.isEmpty()) {
            data = new MarketStats();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, MarketStats.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM market: {}", response, e);
                data = new MarketStats();
            }
        }
        MarketDto dto = new MarketDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static ShipyardDto searchShipyard(long marketId, String orSystemName, String andStationName) {
        if (marketId <= 0 && (orSystemName == null || andStationName == null)) return new ShipyardDto();
        String endpoint = "/api-system-v1/stations/shipyard";
        String response = getServicesResponse(marketId, orSystemName, andStationName, endpoint);
        long timestamp = System.currentTimeMillis();
        ShipyardData data;
        if (response.isEmpty()) {
            data = new ShipyardData();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, ShipyardData.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM shipyard: {}", response, e);
                data = new ShipyardData();
            }
        }
        ShipyardDto dto = new ShipyardDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    public static OutfittingDto searchOutfitting(long marketId, String orSystemName, String andStationName) {
        if (marketId <= 0 && (orSystemName == null || andStationName == null)) return new OutfittingDto();
        String endpoint = "/api-system-v1/stations/outfitting";
        String response = getServicesResponse(marketId, orSystemName, andStationName, endpoint);
        long timestamp = System.currentTimeMillis();
        OutfittingData data;
        if (response.isEmpty()) {
            data = new OutfittingData();
        } else {
            try {
                data = GsonFactory.getGson().fromJson(response, OutfittingData.class);
            } catch (JsonSyntaxException e) {
                log.warn("Invalid JSON from EDSM outfitting: {}", response, e);
                data = new OutfittingData();
            }
        }
        OutfittingDto dto = new OutfittingDto();
        dto.data = data;
        dto.timestamp = timestamp;
        return dto;
    }

    private static String getServicesResponse(long marketId, String orSystemName, String andStationName, String endpoint) {
        StringBuilder query = publicUrl(endpoint);
        try {
            if (marketId > 0) {
                query.append("marketId=").append(marketId);
            } else {
                query.append("systemName=").append(URLEncoder.encode(orSystemName, StandardCharsets.UTF_8));
                query.append("&stationName=").append(URLEncoder.encode(andStationName, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("Failed to encode query parameters", e);
            return "";
        }
        return callEdsm(query);
    }

    private static synchronized void throttle() {
        long minInterval = minIntervalMs();
        if (minInterval <= 0) return;
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime.get();
        if (elapsed < minInterval) {
            SleepNoThrow.sleep(minInterval - elapsed);
        }
        lastRequestTime.set(System.currentTimeMillis());
    }

    private static String callEdsm(StringBuilder query) {
        throttle();
        return doCallEdsm(query, 1);
    }

    private static String doCallEdsm(StringBuilder query, int retriesLeft) {
        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(query.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .GET()
                    .build();


            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            WebSocketBroadcaster.getInstance().broadcast(request);
            int status = response.statusCode();

            if (status == 429) {
                if (retriesLeft > 0) {
                    int resetSecs = response.headers().firstValue("x-rate-limit-reset")
                            .map(v -> {
                                try {
                                    return Integer.parseInt(v);
                                } catch (NumberFormatException e) {
                                    return 10;
                                }
                            })
                            .orElse(10);
                    log.warn("EDSM rate limit hit; waiting {}s before retry", resetSecs + 1);
                    SleepNoThrow.sleep((resetSecs + 1) * 1_000L);
                    throttle();
                    return doCallEdsm(query, retriesLeft - 1);
                }
                log.warn("EDSM API rate limit exceeded after retry");
                return "";
            }
            if (status != 200) {
                log.warn("EDSM API failed with status {}", status);
                return "";
            }

            String body = response.body();
            WebSocketBroadcaster.getInstance().broadcast(body);
            log.debug("EDSM API response: {}", body);
            return body;
        } catch (Exception e) {
            log.error("Failed to query EDSM API: {}", e.getMessage(), e);
            return "";
        }
    }
}