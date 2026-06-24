package elite.intel.companion.llm;

import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;

import java.util.concurrent.CompletableFuture;

/**
 * The single door to the language models for companion mode. Queues {@code LlmRequest}s (never
 * {@code Thought}s), performs native tool-calling, and enforces the tool-call-only contract with a
 * single repair/retry before reporting {@code INVALID_RESPONSE}.
 * <p>
 * Threading: implementations are asynchronous and return immediately with a future.
 */
public interface LlmGateway {

    /**
     * Submits a request for asynchronous processing.
     *
     * @return a future completing with the result ({@link LlmResult.Status#INVALID_RESPONSE} on
     *         unrecoverable bad responses); cancel it to skip (if queued) or discard (if in-flight)
     */
    CompletableFuture<LlmResult> submit(LlmRequest request);

    /**
     * Runs a plain-text compression turn (no tools) for mid-term to long-term memory consolidation.
     *
     * @return a future completing with the model's summary text, or {@code null} when the response is
     *         empty/malformed; no tool-calling contract applies to this turn
     */
    CompletableFuture<String> compressMidTermMemory(LlmRequest request);
}
