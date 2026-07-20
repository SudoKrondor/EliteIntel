package elite.intel.gameapi.search.spansh.traderoute;

import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.util.json.GsonFactory;

public class TradeRouteClient extends SpanshClient {

    private static TradeRouteClient instance;

    private TradeRouteClient() {
        super("https://spansh.co.uk/api/trade/route", "https://spansh.co.uk/api/results/");
    }

    public static synchronized TradeRouteClient getInstance() {
        if (instance == null) {
            instance = new TradeRouteClient();
        }
        return instance;
    }


    public TradeRouteResponse getTradeRoute(TradeRouteSearchCriteria criteria) {
        return GsonFactory.getGson().fromJson(performSearch(criteria), TradeRouteResponse.class);
    }
}
