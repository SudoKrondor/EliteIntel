package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.health.AiServiceCheck;
import elite.intel.ai.brain.health.AiServiceReport;
import elite.intel.ai.brain.health.AiServiceVerdict;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.LlmConnectionStatusEvent;

/**
 * VEGA checks itself when the commander asks: is the AI model answering, and if not, what did the wire see.
 * <p>
 * The answer is spoken from fixed templates ({@link AiServiceReport}), never narrated by the model, because the
 * question is usually asked when the model is what is failing. Its trigger phrases reflex-fire, so it is reached
 * without the model too. None of them says "system": the game already uses that word for ship systems and star
 * systems.
 */
@RegisterQuery
public class SelfDiagnosticQuery implements IntelQuery {

    public static final String ID = "query_vega_self_diagnostic";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Run VEGA's self diagnostic: test the connection to the AI model and report whether it answers "
                + "and how quickly.";
    }

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) {
        AiServiceVerdict verdict = AiServiceCheck.live().run();
        // Keeps the AI tab's connection light in step with what the commander was just told.
        UiBus.publish(new LlmConnectionStatusEvent(verdict.connected()));
        JsonObject response = new JsonObject();
        response.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, AiServiceReport.spoken(verdict));
        return response;
    }
}
