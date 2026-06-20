package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class GeneralConversationQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_general_conversation";


    @Override public String id() { return ID; }

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        return process(
                new AiDataStruct(
                        "Respond naturally to the user's message using your own knowledge",
                        new TextData(originalUserInput)
                ),
                originalUserInput
        );
    }

    record TextData(String text) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}