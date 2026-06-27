package elite.intel.ai.brain.actions.handlers.query;

import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.util.StringUtls;

@RegisterQuery
public class ConnectionCheckQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = AiEndPoint.CONNECTION_CHECK_COMMAND;


    @Override public String id() { return ID; }


    @Override
    public JsonObject handle(String action, JsonObject params, String responseText) {
        // A connectivity probe must structurally confirm a live LLM round-trip. It cannot infer success from
        // the analysis text - on both success and failure analyzeData collapses to a text_to_speech_response -
        // so it asks the endpoint for a structural reachability check (verifyConnection), which fails closed
        // when the endpoint is unreachable. Companion and legacy share the same provider config, so this
        // probes the live endpoint regardless of mode.
        boolean reachable = ApiFactory.getInstance().getAnalysisEndpoint().verifyConnection();
        String key = reachable ? "speech.connectionSuccessful" : "speech.connectionFailed";
        JsonObject response = new JsonObject();
        response.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, StringUtls.localizedLlm(key));
        return response;
    }
}
