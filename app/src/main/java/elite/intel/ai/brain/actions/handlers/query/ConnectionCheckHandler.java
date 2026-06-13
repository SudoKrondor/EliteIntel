package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class ConnectionCheckHandler extends BaseQueryAnalyzer implements QueryHandler {

    @Override
    public JsonObject handle(String action, JsonObject params, String responseText) {


        JsonObject response = process(
                new AiDataStruct("Confirm connection. Respond in requested language. ",
                        new ConnectionCheckData("ping")),
                StringUtls.localizedLlm("connection.check")
        );
        String key = isSuccessfulConnectionCheck(response) ? "speech.connectionSuccessful" : "speech.connectionFailed";
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
