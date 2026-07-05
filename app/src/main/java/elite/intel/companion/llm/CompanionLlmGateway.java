package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.tools.ClassifyTurnFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Provider-neutral {@link LlmGateway}: orchestrates render -> send -> parse via the injected
 * {@link LlmProviderAdapter} and {@link LlmTransport}, enforces the tool-call-only contract, and does a
 * single repair/retry before reporting {@link LlmResult.Status#INVALID_RESPONSE}. A response is valid
 * only when it is one or more tool-calls whose names were actually offered this turn.
 * <p>
 * It also enforces the classify-first protocol: when {@code classify_turn} is among the offered tools (a
 * classifying turn - narration never offers it), a response without a {@code classify_turn} call draws the
 * same single repair/retry with a targeted nudge. This omission is not fatal: if the retry still lacks it,
 * the structurally valid response is returned anyway (the turn settles; only the memory stamping degrades
 * to defaults), never downgraded to INVALID.
 * <p>
 * Threading: requests run on a single-thread executor (consciousness is serialized); {@link #submit}
 * returns immediately with a future.
 */
public final class CompanionLlmGateway implements LlmGateway {

    private static final Logger log = LogManager.getLogger(CompanionLlmGateway.class);

    private static final LlmResult INVALID = new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());

    private final LlmProviderAdapter adapter;
    private final LlmTransport transport;
    private final Executor executor;

    public CompanionLlmGateway(LlmProviderAdapter adapter, LlmTransport transport) {
        this(adapter, transport, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-llm");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** Test seam: inject a synchronous/controlled executor. */
    CompanionLlmGateway(LlmProviderAdapter adapter, LlmTransport transport, Executor executor) {
        this.adapter = adapter;
        this.transport = transport;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<LlmResult> submit(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> process(request), executor);
    }

    @Override
    public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
        // Plain-text turn (request carries no tools): render -> send -> extract text; null on bad output.
        return CompletableFuture.supplyAsync(() -> adapter.parseText(transport.send(adapter.buildRequestBody(request))), executor);
    }

    /**
     * Shuts down the owned executor so a short-lived gateway does not leak its thread. The long-lived
     * companion-runtime gateway is never closed; only callers that build a throwaway gateway (e.g. custom
     * command key generation) close it, after their single blocking call has returned. The injected-executor
     * test seam passes a non-{@link ExecutorService} executor, for which this is a no-op.
     */
    @Override
    public void close() {
        if (executor instanceof ExecutorService service) {
            service.shutdownNow();
        }
    }

    /**
     * What is wrong with a parsed response; drives the repair nudge and the fallback. NONE means no defect;
     * MALFORMED (not offered tool-calls at all) is more severe than MISSING_CLASSIFY (usable but unclassified).
     */
    private enum Defect { NONE, MALFORMED, MISSING_CLASSIFY }

    private LlmResult process(LlmRequest request) {
        LlmResult first = attempt(request);
        Defect firstDefect = defectOf(first, request);
        if (firstDefect == Defect.NONE) {
            return first;
        }
        // Single repair/retry with a defect-targeted nudge.
        log.warn("LLM response not usable ({}, status={}, tool-calls={}); retrying once",
                firstDefect, first.status(), first.toolInvocations().size());
        LlmResult second = attempt(repair(request, firstDefect));
        Defect secondDefect = defectOf(second, request);
        if (secondDefect == Defect.NONE) {
            return second;
        }
        // Graceful degradation: a response that only lacks classify_turn still settles the turn for the
        // commander (memory stamping falls back to defaults) - better than a service-phrase INVALID.
        if (firstDefect == Defect.MISSING_CLASSIFY) {
            log.warn("classify_turn still missing after retry; accepting the response without it");
            return first;
        }
        if (secondDefect == Defect.MISSING_CLASSIFY) {
            log.warn("classify_turn missing in the repaired response; accepting it without classification");
            return second;
        }
        log.warn("LLM response still invalid after retry; returning INVALID_RESPONSE");
        return INVALID;
    }

    private LlmResult attempt(LlmRequest request) {
        String body = adapter.buildRequestBody(request);
        JsonObject response = transport.send(body);
        return adapter.parse(response);
    }

    /**
     * Classifies a parsed response: {@link Defect#MALFORMED} when it is not one-or-more offered tool-calls;
     * {@link Defect#MISSING_CLASSIFY} when the turn offered {@code classify_turn} (a classifying turn) but the
     * response does not call it; {@link Defect#NONE} otherwise. A bare {@code classify_turn} alone is NONE here -
     * whether that settles the turn is the thought's business, not the protocol's.
     */
    private Defect defectOf(LlmResult result, LlmRequest request) {
        if (!result.isValid() || result.toolInvocations().isEmpty()) {
            return Defect.MALFORMED;
        }
        Set<String> offered = request.tools().stream()
                .map(LlmToolDefinition::name)
                .collect(Collectors.toSet());
        boolean allOffered = result.toolInvocations().stream()
                .map(LlmToolInvocation::name)
                .allMatch(offered::contains);
        if (!allOffered) {
            return Defect.MALFORMED;
        }
        boolean classifyRequired = offered.contains(ClassifyTurnFunction.ID);
        boolean classifyCalled = result.toolInvocations().stream()
                .anyMatch(inv -> ClassifyTurnFunction.ID.equals(inv.name()));
        return classifyRequired && !classifyCalled ? Defect.MISSING_CLASSIFY : Defect.NONE;
    }

    private LlmRequest repair(LlmRequest request, Defect defect) {
        String nudge = defect == Defect.MISSING_CLASSIFY
                ? "Your previous response omitted the mandatory 'classify_turn' call. Resend it: call"
                + " 'classify_turn' first, then the same settling function call."
                : "Your previous response was not a valid function call. Respond only by calling one of the provided functions.";
        List<LlmMessage> messages = new ArrayList<>(request.messages());
        messages.add(LlmMessage.of(LlmMessageRole.USER, nudge));
        return new LlmRequest(request.requestId(), messages, request.tools(), request.profile());
    }
}
