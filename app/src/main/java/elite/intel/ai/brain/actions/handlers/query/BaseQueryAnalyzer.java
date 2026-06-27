package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;
import elite.intel.ws.WebSocketBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
