package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GsonFactory;
import elite.intel.ws.WebSocketBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shared base for query handlers, owning the query result contract. The shape the consciousness / legacy
 * router receives is consistent by meaning:
 * <ul>
 *   <li>{@link #process(AiData, String)} and {@link #process(String, Object)} - the query has data:
 *       companion gets {@code {data: <raw fields>}} ({@link AIConstants#PROPERTY_DATA}) to narrate itself;
 *       legacy phrases it (analysis LLM for {@code AiData}, or the supplied canned localized sentence for
 *       the dual-path).</li>
 *   <li>{@link #process(String)} - a short status / inherently-textual answer (no data, error, help,
 *       conversation): both modes get {@code {text_to_speech_response: ...}} spoken directly.</li>
 * </ul>
 * So a companion query result carries {@code data} when there are fields to reason over, otherwise a direct
 * {@code text_to_speech_response} - never a pre-formatted prose sentence dressed up as data.
 */
public class BaseQueryAnalyzer {

    private static final Logger log = LogManager.getLogger(BaseQueryAnalyzer.class);

    protected JsonObject process(AiData struct, String originalUserInput) {

        log.info("Processing data: \n\n{}\n\n", struct.getData().toYaml());
        if(originalUserInput == null) {originalUserInput = "";}

        // Companion mode owns narration: hand back the raw data for the consciousness to phrase, skipping the
        // legacy second-pass analysis LLM (and its TTS-formatted response). Legacy keeps the analysis pass.
        if (SystemSession.getInstance().companionModeOn()) {
            JsonObject result = new JsonObject();
            result.add(AIConstants.PROPERTY_DATA, GsonFactory.getGson().toJsonTree(struct.getData()));
            return result;
        }

        // Legacy: the second-pass analysis LLM is slow, so give the commander a "stand by" while it runs.
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("query.analyzing")));
        AiAnalysisInterface aiAnalysisInterface = ApiFactory.getInstance().getAnalysisEndpoint();
        JsonObject analysis = aiAnalysisInterface.analyzeData(originalUserInput, struct);
        WebSocketBroadcaster.getInstance().broadcast(analysis);

        if (!analysis.has(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE)) {
            analysis = GenericResponse.getInstance().genericResponse("LLM failed to process this request.");
        }
        return analysis;
    }

    /**
     * Dual-path result for a query that already computes a deterministic spoken sentence: legacy speaks the
     * canned {@code legacySpokenText} directly (no analysis LLM), while the companion receives
     * {@code companionData} as raw fields under {@code data} to narrate itself. Keeps the companion contract
     * consistent (data when there are fields) without changing legacy's localized phrasing.
     */
    protected JsonObject process(String legacySpokenText, Object companionData) {
        if (SystemSession.getInstance().companionModeOn()) {
            JsonObject result = new JsonObject();
            result.add(AIConstants.PROPERTY_DATA, GsonFactory.getGson().toJsonTree(companionData));
            return result;
        }
        return process(legacySpokenText);
    }

    protected JsonObject process(String message) {
        JsonObject object = new JsonObject();
        object.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, message);
        WebSocketBroadcaster.getInstance().broadcast(object);
        return object;
    }
}
