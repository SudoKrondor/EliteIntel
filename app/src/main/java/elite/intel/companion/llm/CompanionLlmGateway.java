package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.tools.ClassifyTurnFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Provider-neutral {@link LlmGateway}: orchestrates render -> send -> parse via the injected
 * {@link LlmProviderAdapter} and {@link LlmTransport}, enforces the tool-call-only contract, and does a
 * single repair/retry before reporting {@link LlmResult.Status#INVALID_RESPONSE}. A response is valid
 * only when it is one or more tool-calls whose names were actually offered this turn.
 * <p>
 * It also enforces the classify-first protocol: when {@code classify_turn} is among the offered tools (a
 * classifying turn - narration never offers it), a response without a {@code classify_turn} call draws the
 * same single repair/retry. When the partial response contains valid offered tool-calls, the retry replays them
 * as an {@code assistant(tool_calls) -> tool(result)} continuation and rejects them without execution. This keeps
 * the system prompt and commander history unchanged while giving the model protocol-valid feedback.
 * <p>
 * Symmetrically, a classifying turn must also carry a settling call: a response that calls
 * {@code classify_turn} and nothing else has classified the turn but neither answered nor acted, so it
 * falls silent. It receives the same repair. The repair transcript is request-local and is never written to
 * durable memory; only a clean retry reaches the thought for execution.
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

    /** What is wrong with a parsed response. {@link #NONE} means the response is ready for execution. */
    private enum Defect {
        NONE, MISSING_CLASSIFY, MISSING_SETTLING, MALFORMED
    }

    /** A single send/parse round paired with the defect it was found to have. */
    private record Attempt(LlmResult result, Defect defect) {}

    private LlmResult process(LlmRequest request) {
        Attempt first = attempt(request, 1);
        if (first.defect() == Defect.NONE) {
            return first.result();
        }
        // One protocol-valid repair/retry. Surface it on the diagnostics surface (attributed to the owning
        // thought's trace) so this second physical call is visible as part of the round.
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        String retryKind = canContinue(first) ? "assistant/tool continuation" : "unchanged request";
        CompanionDiagnostics.debug(trace, "llm", "attempt#1 " + first.defect() + " -> retry (" + retryKind + ")");
        log.warn("LLM response has defect {} (status={}, tool-calls={}); retrying once via {}",
                first.defect(), first.result().status(), first.result().toolInvocations().size(), retryKind);
        Attempt second = attempt(repair(request, first), 2);

        if (second.defect() == Defect.NONE) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#2 ok");
            return second.result();
        }
        CompanionDiagnostics.debug(trace, "llm", "attempt#2 still " + second.defect() + " -> INVALID");
        log.warn("LLM response still has defect {} after retry; returning INVALID_RESPONSE", second.defect());
        return INVALID;
    }

    private Attempt attempt(LlmRequest request, int attemptNumber) {
        long attemptStartedNanos = System.nanoTime();
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        try {
            long renderStartedNanos = System.nanoTime();
            String body = adapter.buildRequestBody(request);
            long renderMillis = elapsedMillis(renderStartedNanos);
            long httpStartedNanos = System.nanoTime();
            JsonObject response = transport.send(body);
            long httpMillis = elapsedMillis(httpStartedNanos);
            long parseStartedNanos = System.nanoTime();
            LlmResult result = adapter.parse(response);
            long parseMillis = elapsedMillis(parseStartedNanos);
            CompanionDiagnostics.debug(trace, "llm-http",
                    "attempt=" + attemptNumber
                            + " render=" + renderMillis + " ms"
                            + " http=" + httpMillis + " ms"
                            + " parse=" + parseMillis + " ms"
                            + " total=" + elapsedMillis(attemptStartedNanos) + " ms");
            return new Attempt(result, defectOf(result, request));
        } catch (RuntimeException failure) {
            CompanionDiagnostics.debug(trace, "llm-http",
                    "attempt=" + attemptNumber + " failed after " + elapsedMillis(attemptStartedNanos) + " ms");
            throw failure;
        }
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    /**
     * Classifies a parsed response: {@link Defect#MALFORMED} when it is not one-or-more offered tool-calls;
     * {@link Defect#MISSING_CLASSIFY} when the turn offered {@code classify_turn} (a classifying turn) but the
     * response does not call it; {@link Defect#MISSING_SETTLING} when it calls {@code classify_turn} and nothing
     * else (the turn is classified but neither answered nor acted, so it would fall silent); {@link Defect#NONE}
     * otherwise. A classifying turn that called {@code classify_turn} is either NONE (a settling call is also
     * present) or MISSING_SETTLING (classify-only); the two cannot both hold.
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
        if (!offered.contains(ClassifyTurnFunction.ID)) {
            return Defect.NONE; // not a classifying turn (e.g. narration): no classify/settling protocol
        }
        boolean classifyCalled = result.toolInvocations().stream()
                .anyMatch(inv -> ClassifyTurnFunction.ID.equals(inv.name()));
        if (!classifyCalled) {
            return Defect.MISSING_CLASSIFY;
        }
        // A settling call is any invocation other than classify_turn (speak, or an action command/query).
        boolean settlingCalled = result.toolInvocations().stream()
                .anyMatch(inv -> !ClassifyTurnFunction.ID.equals(inv.name()));
        return settlingCalled ? Defect.NONE : Defect.MISSING_SETTLING;
    }

    /**
     * Builds a native tool continuation for a structurally valid but incomplete response. Each replayed call gets
     * a truthful {@code rejected} result: no call has executed or changed state. Invalid or unoffered calls cannot
     * form a valid continuation, so their retry keeps the original request unchanged.
     */
    private LlmRequest repair(LlmRequest request, Attempt failedAttempt) {
        if (!canContinue(failedAttempt)) {
            return request;
        }
        List<LlmToolInvocation> replayedCalls = withReplayIds(failedAttempt.result().toolInvocations());
        List<LlmMessage> messages = new ArrayList<>(request.messages());
        messages.add(LlmMessage.assistantToolCalls(replayedCalls));
        String rejection = rejectionFor(failedAttempt.defect());
        for (LlmToolInvocation call : replayedCalls) {
            messages.add(LlmMessage.toolResult(call.id(), rejection));
        }
        return new LlmRequest(request.requestId(), List.copyOf(messages), request.tools(), request.profile(), request.trace());
    }

    private static boolean canContinue(Attempt failedAttempt) {
        return failedAttempt.defect() != Defect.MALFORMED
                && failedAttempt.result().isValid()
                && !failedAttempt.result().toolInvocations().isEmpty();
    }

    /** Assigns unique request-local ids when a provider omitted or duplicated them, keeping every result linked. */
    private static List<LlmToolInvocation> withReplayIds(List<LlmToolInvocation> calls) {
        List<LlmToolInvocation> replayed = new ArrayList<>(calls.size());
        Set<String> usedIds = new HashSet<>();
        int nextSyntheticId = 1;
        for (LlmToolInvocation call : calls) {
            String id = call.id();
            if (id == null || id.isBlank() || !usedIds.add(id)) {
                do {
                    id = "repair-rejected-call-" + nextSyntheticId++;
                } while (!usedIds.add(id));
            }
            replayed.add(new LlmToolInvocation(id, call.name(), call.arguments()));
        }
        return List.copyOf(replayed);
    }

    /** Returns a truthful tool result for a call that was never executed. */
    private static String rejectionFor(Defect defect) {
        return switch (defect) {
            case MISSING_CLASSIFY -> "{\"status\":\"rejected\",\"reason\":\"classify_turn must be called before a settling function\"}";
            case MISSING_SETTLING -> "{\"status\":\"rejected\",\"reason\":\"a settling function must follow classify_turn\"}";
            case NONE, MALFORMED -> throw new IllegalArgumentException("No tool continuation for " + defect);
        };
    }
}
