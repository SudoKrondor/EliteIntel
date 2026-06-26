package elite.intel.ai.brain.actions.handlers.query;

import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class ConnectionCheckQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = AiEndPoint.CONNECTION_CHECK_COMMAND;


    @Override public String id() { return ID; }


    @Override
    public JsonObject handle(String action, JsonObject params, String responseText) {
        // A connectivity probe must actually round-trip the LLM, so it calls the analysis endpoint directly
        // instead of the mode-aware BaseQueryAnalyzer.process(AiData): in companion mode that method skips the
        // LLM call (returns raw data for the consciousness to narrate), which would make every check report a
        // false failure. Companion and legacy share the same provider config, so this probes the live endpoint.
        JsonObject probe = ApiFactory.getInstance().getAnalysisEndpoint().analyzeData(
                StringUtls.localizedLlm("connection.check"),
                new AiDataStruct("Confirm connection. Respond in requested language. ",
                        new ConnectionCheckData("ping")));
        String key = isSuccessfulConnectionCheck(probe) ? "speech.connectionSuccessful" : "speech.connectionFailed";
        JsonObject response = new JsonObject();
        response.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, StringUtls.localizedLlm(key));
        return response;
    }

    private boolean isSuccessfulConnectionCheck(JsonObject response) {
        if (response == null || !response.has(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE)) {
            return false;
        }
        String text = response.get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE).getAsString();
        return !text.startsWith("LLM failed to process this request.");
    }

    record ConnectionCheckData(String data) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
