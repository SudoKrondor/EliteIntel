package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.db.managers.BountyManager;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeBountiesCollectedQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_total_bounties";

    @Override
    public String llmDescription() {
        return "Report the total bounty vouchers/credits the commander has collected this session.";
    }


    @Override public String id() { return ID; }


    private final BountyManager bountyManager = BountyManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        // Sum only bounties not yet cashed in - answers "how much uncollected bounty do I have?"
        long totalBounties = bountyManager.getAll().stream()
                .filter(b -> !b.isCashedIn())
                .mapToLong(b -> b.getTotalReward())
                .sum();

        String instructions = """
                Report the totalBounties value as credits collected in bounties this session.
                State only the amount. No commentary.
                """;
        return process(new AiDataStruct(instructions, new DataDto(totalBounties)), originalUserInput);
    }

    record DataDto(long totalBounties) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
