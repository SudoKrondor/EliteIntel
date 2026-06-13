package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;

public class AnalyzeTradeScheduleHandler extends BaseQueryAnalyzer implements QueryHandler {

    public static final String INSTRUCTIONS = """
            Answer the user's question about the current trade route schedule.
            
            Data format (one line per leg):
            Leg: <number>, Source System: <system>, Station: <station>, Commodities: [<list>], Destination System: <system>, Destination Station: <station>
            
            Rules:
            - Answer only what the user asked.
            - If asked about a specific leg: find the matching leg number and report its details.
            - If asked what commodities are being traded: list the commodities from all legs or a specific leg.
            - If asked about source or destination: report the system and station names.
            """;

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();
        List<TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto>> allStops = tradeRouteManager.getAllStops();

        StringBuilder sb = new StringBuilder();
        for (TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto> tuple : allStops) {
            sb.append("Leg: ");
            sb.append(tuple.getLegNumber())
                    .append(", Source System: ")
                    .append(tuple.getTradeStopDto().getSourceSystem())
                    .append(", Station: ")
                    .append(tuple.getTradeStopDto().getSourceStation())
                    .append(", Commodities: [")
                    .append(tuple.getTradeStopDto().getCommodities().stream().collect(() -> new StringBuilder(), (sb1, commodity) -> sb1.append(commodity.getName()).append(", "), StringBuilder::append))
                    .append("]")
                    .append(", Destination System: ")
                    .append(tuple.getTradeStopDto().getDestinationSystem())
                    .append(", Destination Station: ")
                    .append(tuple.getTradeStopDto().getDestinationStation())
                    .append("\n");
        }
        if(allStops.isEmpty()){
            return process(StringUtls.localizedLlm("query.trade.noSchedule"));
        }
        return process(new AiDataStruct(INSTRUCTIONS, new DataDto(sb.toString())), originalUserInput);
    }

    record DataDto(String data) implements ToYamlConvertable {

        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
