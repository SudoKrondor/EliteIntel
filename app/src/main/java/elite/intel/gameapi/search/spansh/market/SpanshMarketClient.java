package elite.intel.gameapi.search.spansh.market;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpanshMarketClient extends SpanshClient {

    private static final String SAVE_URL = "https://spansh.co.uk/api/stations/search/save";
    private static final String RESULTS_URL = "https://spansh.co.uk/api/stations/search/recall/";
    private static final Logger log = LogManager.getLogger(SpanshMarketClient.class);

    public SpanshMarketClient() {
        super(SAVE_URL, RESULTS_URL);
    }

    public List<StationMarketDto> searchMarkets(MarketSearchCriteria criteria) {

        // Build POST payload
        JsonObject filters = new JsonObject();
        JsonObject distance = new JsonObject();
        distance.addProperty("min", criteria.minDistance());
        distance.addProperty("max", criteria.maxDistance());
        filters.add("distance", distance);

        JsonArray market = new JsonArray();
        JsonObject commodity = new JsonObject();
        commodity.addProperty("name", criteria.commodityName());
        market.add(commodity);
        filters.add("market", market);

        JsonObject payload = new JsonObject();
        payload.add("filters", filters);
        payload.add("sort", new JsonArray());
        payload.addProperty("page", 0);
        payload.addProperty("size", 100); // 100 results per page
        payload.addProperty("reference_system", criteria.referenceSystem());


        JsonObject result = performSearch(GsonFactory.getGson().toJson(payload));
        if (result == null) return new ArrayList<>();
        log.debug("Result: {}", GsonFactory.getGson().toJson(result));

        // Parse and filter results
        JsonArray resultsArray = result.getAsJsonArray("results");
        List<StationMarketDto> stations = new ArrayList<>();
        if (resultsArray == null) return stations;

        for (var stationElement : resultsArray) {
            JsonObject stationJson = stationElement.getAsJsonObject();

            // Apply station-level filters
            if (criteria.requireLargePad() && !stationJson.get("has_large_pad").getAsBoolean()) {
                continue;
            }
            if (criteria.requirePlanetary() != null &&
                    criteria.requirePlanetary() != stationJson.get("is_planetary").getAsBoolean()) {
                continue;
            }

            // Check market for the commodity
            JsonArray marketArray = stationJson.getAsJsonArray("market");
            if (marketArray != null) {
                for (var itemElement : marketArray) {
                    JsonObject item = itemElement.getAsJsonObject();
                    if (
                            criteria.commodityName().equalsIgnoreCase(item.get("commodity").getAsString())
                                    && (!criteria.requireSupply() || item.get("supply").getAsInt() > 0)
                                    && (criteria.wantToBuy() ? item.get("sell_price").getAsInt() > 0 : item.get("buy_price").getAsInt() > 0)
                                    && (criteria.minSupply() <= 0 || item.get("supply").getAsInt() >= criteria.minSupply())
                    ) {

                        StationMarketDto station = new StationMarketDto();
                        station.setMarketId(stationJson.get("market_id").getAsLong());
                        station.setStationName(stationJson.get("name").getAsString());
                        station.setSystemName(stationJson.get("system_name").getAsString());
                        station.setDistance(Math.round(stationJson.get("distance").getAsDouble() * 10.0) / 10.0);
                        station.setHasLargePad(stationJson.get("has_large_pad").getAsBoolean());
                        station.setPlanetary(stationJson.get("is_planetary").getAsBoolean());
                        station.setMarketUpdatedAt(stationJson.get("market_updated_at").getAsString());
                        station.setSellPrice(item.get("sell_price").getAsInt());
                        station.setBuyPrice(item.get("buy_price").getAsInt());
                        station.setCommoditySupply(item.get("supply").getAsInt());
                        stations.add(station);
                        break; // Found the commodity, no need to check further
                    }
                }
            }
        }

        if (criteria.orderByDistance())
            stations.sort(Comparator.comparing(StationMarketDto::distance));
        else if (criteria.wantToBuy())
            stations.sort(Comparator.comparing(StationMarketDto::getSellPrice));
        else
            stations.sort(Comparator.comparing(StationMarketDto::getBuyPrice).reversed());

        return stations;
    }
}