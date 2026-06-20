package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeTradeProfileQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_trade_profile";


    @Override public String id() { return ID; }


    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing trade profile. Stand by."));
        TradeProfileManager tradeProfileManager = TradeProfileManager.getInstance();
        TradeRouteSearchCriteria criteria = tradeProfileManager.getCriteria(false);
        String instructions = """
                Answer the user's question about the current trade profile configuration.
                
                Field notes:
                - startingBudget: if zero, the profile is not configured yet
                - maxStationDistanceLs: maximum station distance in light seconds from the arrival star (in-system only, never light years)
                - priceAge: given in seconds, convert to hours and minutes when reporting
                - hops: measured in light years (distance between star systems)
                - allowStrongHold: whether stops at enemy powerplay strongholds are permitted
                
                Rules:
                - Answer only what the user asked.
                - Do not include system name or station name in the response.
                - If startingBudget is zero, state the profile is not configured.
                """;
        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(criteria.toJsonForAnalysis())
                ),
                originalUserInput
        );
    }

    record DataDto(String criteria) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
