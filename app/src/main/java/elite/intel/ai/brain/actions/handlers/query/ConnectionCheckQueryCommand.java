package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;

import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.LlmConnectionStatusEvent;
import elite.intel.util.StringUtls;

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
        boolean reachable = ApiFactory.getInstance().getAnalysisEndpoint().verifyConnection();
        String key = reachable ? "speech.connectionSuccessful" : "speech.connectionFailed";
        UiBus.publish(new LlmConnectionStatusEvent(reachable));
        JsonObject response = new JsonObject();
        response.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, StringUtls.localizedLlm(key));
        return response;
    }
}
