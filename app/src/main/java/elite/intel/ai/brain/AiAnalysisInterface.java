package elite.intel.ai.brain;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;

/**
 * Provides a contract for analyzing data based on user intent and raw data input.
 * The implementation of this interface is expected to perform domain-specific
 * data analysis and return the resulting output within a structured JSON format.
 */
public interface AiAnalysisInterface {
    JsonObject analyzeData(String userIntent, AiData data);

    /**
     * Performs a minimal live round-trip to the configured LLM backend and reports whether it
     * answered with a usable completion. Used by the startup/connectivity check, which cannot infer
     * reachability from {@link #analyzeData} text alone (success and failure both collapse to a
     * text_to_speech_response). Implementations must fail closed: any transport error, HTTP error,
     * or malformed/empty response returns false.
     *
     * @return true only when the backend is reachable and returned a completion envelope.
     */
    boolean verifyConnection();
}
