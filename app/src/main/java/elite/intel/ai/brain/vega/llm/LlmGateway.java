package elite.intel.ai.brain.vega.llm;

import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.LlmResult;

import java.util.concurrent.CompletableFuture;

/**
 * The single door to the language models for companion mode. Queues {@code LlmRequest}s (never
 * {@code Thought}s), performs native tool-calling, and enforces the tool-call-only contract with one protocol
 * repair for invalid model output. A transient HTTP failure gets a short ladder of jittered resends; a
 * transport failure that outlives the ladder - and any permanent one - reports
 * {@code SERVICE_UNAVAILABLE} without a protocol repair, which is how a caller tells an unreachable
 * provider from a provider that answered badly.
 * <p>
 * Threading: implementations are asynchronous and return immediately with a future.
 * <p>
 * Extends {@link AutoCloseable} so a caller that constructs a short-lived gateway (rather than the
 * long-lived one the companion runtime holds) can release its executor deterministically via
 * try-with-resources. {@link #close()} defaults to a no-op for implementations that own no resources.
 */
public interface LlmGateway extends AutoCloseable {

    /**
     * Submits a request for asynchronous processing.
     *
     * @return a future completing with the result ({@link LlmResult.Status#INVALID_RESPONSE} on
     *         unrecoverable bad responses, {@link LlmResult.Status#SERVICE_UNAVAILABLE} when the provider
     *         never answered); cancel it to skip a queued request or interrupt its in-flight
     *         physical provider exchange
     */
    CompletableFuture<LlmResult> submit(LlmRequest request);

    /**
     * Runs a plain text-in/text-out turn (no tools), for the callers that want a phrase back rather than an action.
     *
     * @return a future completing with the model's text, or {@code null} when the response is
     *         empty/malformed; no tool-calling contract applies to this turn, and cancellation follows
     *         the same queued/in-flight behavior as {@link #submit(LlmRequest)}
     */
    CompletableFuture<String> completePlainText(LlmRequest request);

    /**
     * Releases any resources the gateway owns (e.g. an executor). No-op by default.
     */
    @Override
    default void close() {
    }
}
