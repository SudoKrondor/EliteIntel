package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;
import elite.intel.ws.WebSocketBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shared base for query handlers. A query's outcome is deterministic and owned by the handler: it always
 * resolves to a spoken {@code text_to_speech_response} that the active mode owner vocalizes verbatim - the
 * legacy {@code ResponseRouter} in command mode, the companion {@code Thought} in companion mode. The mode
 * never re-decides whether or what a query says.
 * <ul>
 *   <li>{@link #process(AiData, String)} - the query has data fields: the analysis LLM phrases them into a
 *       spoken sentence (the analysis <em>result</em> is what gets vocalized, not the raw fields).</li>
 *   <li>{@link #process(String)} - a short, inherently-textual answer (status, error, help, no data):
 *       spoken directly with no analysis pass.</li>
 * </ul>
 * Both modes receive the same {@code {text_to_speech_response: ...}} shape; the companion does not get raw
 * data to phrase itself, so a query never falls silent at the LLM's discretion.
 */
public class BaseQueryAnalyzer {

    private static final Logger log = LogManager.getLogger(BaseQueryAnalyzer.class);

    protected JsonObject process(AiData struct, String originalUserInput) {

        log.info("Processing data: \n\n{}\n\n", struct.getData().toYaml());
        if (originalUserInput == null) {
            originalUserInput = "";
        }

        AiAnalysisInterface aiAnalysisInterface = ApiFactory.getInstance().getAnalysisEndpoint();
        JsonObject analysis = aiAnalysisInterface.analyzeData(originalUserInput, struct);
        WebSocketBroadcaster.getInstance().broadcast(analysis);

        if (!analysis.has(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE)) {
            analysis = GenericResponse.getInstance().genericResponse("LLM failed to process this request.");
        }
        return analysis;
    }

    protected JsonObject process(String message) {
        JsonObject object = new JsonObject();
        object.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, message);
        WebSocketBroadcaster.getInstance().broadcast(object);
        return object;
    }
}
