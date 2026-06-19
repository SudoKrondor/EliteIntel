package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.dto.MarketDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class AnalyzeLocalMarketsHandler extends BaseQueryAnalyzer implements IntelQuery {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public String id() { return "analyze_local_markets_handler"; }

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing local market data. Stand by."));
        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        MarketDto market = currentLocation.getMarket();
        if (market == null || market.getData() == null) {
            return process(StringUtls.localizedLlm("query.market.noData"));
        }

        String instructions = """
                Answer the user's question about the market at the current station.
                
                Data fields:
                - market.data.name: station name
                - market.data.sName: star system name
                - market.data.commodities: list of commodities available at this market
                  - name: commodity name
                  - buyPrice: price to buy from the station (zero if not sold here)
                  - stock: units available to buy
                  - sellPrice: price the station pays when you sell (zero if not buying)
                  - demand: units the station wants to buy
                  - stockBracket: supply level indicator (zero = not stocked)
                
                Rules:
                - If asked about a specific commodity: find it by name and report buy price, sell price, stock, and demand.
                - If asked what is available to buy: list commodities where stock is greater than zero.
                - If asked what the station is buying: list commodities where demand is greater than zero.
                - Answer only what was asked.
                """;
        return process(new AiDataStruct(instructions, new DataDto(market)), originalUserInput);
    }

    private record DataDto(MarketDto market) implements ToYamlConvertable{
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
