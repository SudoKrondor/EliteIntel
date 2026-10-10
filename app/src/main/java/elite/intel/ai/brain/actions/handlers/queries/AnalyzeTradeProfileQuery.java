package elite.intel.ai.brain.actions.handlers.queries;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.ai.brain.vega.SpokenAmounts;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeTradeProfileQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_trade_profile";

    @Override
    public String llmDescription() {
        return "Report the configured trade-route search parameters (budget, max stops, max distance, and the permit/planetary-port/prohibited-cargo/stronghold toggles).";
    }


    @Override public String id() { return ID; }


    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing trade profile. Stand by."));
        TradeProfileManager tradeProfileManager = TradeProfileManager.getInstance();
        TradeRouteSearchCriteria criteria = tradeProfileManager.getCriteria(false);
        String instructions = """
                Answer the user's question about the current trade profile configuration.
                
                Field notes:
                - startingCapital: the trade budget in credits. If zero, the profile is not configured yet
                - maxStationDistanceLs: maximum station distance in light seconds from the arrival star (in-system only, never light years)
                - priceAge: given in seconds, convert to hours and minutes when reporting
                - hops: measured in light years (distance between star systems)
                - allowStrongHold: whether stops at enemy powerplay strongholds are permitted
                
                Rules:
                - Answer only what the user asked.
                - Do not include system name or station name in the response.
                - If startingCapital is zero, state the profile is not configured.
                """ + SpokenAmounts.RULE;
        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(criteria.toJsonForAnalysis(), criteria.getStartingCapital())
                ),
                originalUserInput
        );
    }

    record DataDto(String criteria, @JsonIgnore long startingCapital) implements ToYamlConvertable {
        @Override public String toYaml() {
            // The budget sits inside the criteria JSON, so its spoken form rides alongside it.
            return YamlFactory.toYaml(this) + SpokenAmounts.yamlLine("startingCapital", startingCapital);
        }
    }
}
