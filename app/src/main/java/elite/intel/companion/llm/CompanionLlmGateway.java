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
 * A classifying turn may arrive in two native tool-call rounds: {@code classify_turn} first, then its settling
 * call after a tool result. A classify-only response is therefore not repaired or executed. The gateway replays
 * it with a truthful pending result, makes one settling-only follow-up request, and returns the expected ordered
 * pair to the thought. The protocol transcript is request-local and is never written to durable memory.
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

    /** Which tool-call shape the current physical LLM round must produce. */
    private enum RoundExpectation {
        INITIAL,
        SETTLING_AFTER_CLASSIFY
    }

    /** A single send/parse round paired with the defect it was found to have. */
    private record Attempt(LlmResult result, Defect defect) {}

    private LlmResult process(LlmRequest request) {
        Attempt first = attempt(request, 1, RoundExpectation.INITIAL);
        if (first.defect() == Defect.NONE) {
            return first.result();
        }
        if (first.defect() == Defect.MISSING_SETTLING) {
            return continueAfterClassification(request, first, 2);
        }
        // One protocol-valid repair/retry. Surface it on the diagnostics surface (attributed to the owning
        // thought's trace) so this second physical call is visible as part of the round.
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        String retryKind = canContinue(first) ? "assistant/tool continuation" : "unchanged request";
        CompanionDiagnostics.debug(trace, "llm", "attempt#1 " + first.defect() + " -> retry (" + retryKind + ")");
        log.warn("LLM response has defect {} (status={}, tool-calls={}); retrying once via {}",
                first.defect(), first.result().status(), first.result().toolInvocations().size(), retryKind);
        LlmRequest repaired = repair(request, first);
        Attempt second = attempt(repaired, 2, RoundExpectation.INITIAL);

        if (second.defect() == Defect.NONE) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#2 ok");
            return second.result();
        }
        if (second.defect() == Defect.MISSING_SETTLING) {
            return continueAfterClassification(repaired, second, 3);
        }
        CompanionDiagnostics.debug(trace, "llm", "attempt#2 still " + second.defect() + " -> INVALID");
        log.warn("LLM response still has defect {} after retry; returning INVALID_RESPONSE", second.defect());
        return INVALID;
    }

    /** Runs one physical provider request and validates it for the expected position in the local tool flow. */
    private Attempt attempt(LlmRequest request, int attemptNumber, RoundExpectation expectation) {
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
            return new Attempt(result, defectOf(result, request, expectation));
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
     * response does not call it; {@link Defect#MISSING_SETTLING} when it calls exactly one
     * {@code classify_turn} and nothing else (a valid native continuation point); {@link Defect#NONE} when the
     * classifying turn contains exactly one classify and exactly one settling call. A settling follow-up must
     * contain exactly one non-classify offered call.
     */
    private Defect defectOf(LlmResult result, LlmRequest request, RoundExpectation expectation) {
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
        if (expectation == RoundExpectation.SETTLING_AFTER_CLASSIFY) {
            return result.toolInvocations().size() == 1 && !isClassify(result.toolInvocations().get(0))
                    ? Defect.NONE
                    : Defect.MALFORMED;
        }
        if (!offered.contains(ClassifyTurnFunction.ID)) {
            return Defect.NONE; // not a classifying turn (e.g. narration): no classify/settling protocol
        }
        long classifyCalls = result.toolInvocations().stream()
                .filter(CompanionLlmGateway::isClassify)
                .count();
        if (classifyCalls == 0) {
            return Defect.MISSING_CLASSIFY;
        }
        if (classifyCalls != 1) {
            return Defect.MALFORMED;
        }
        // A settling call is any invocation other than classify_turn (speak, or an action command/query).
        long settlingCalls = result.toolInvocations().stream()
                .filter(inv -> !ClassifyTurnFunction.ID.equals(inv.name()))
                .count();
        if (settlingCalls == 0) {
            return Defect.MISSING_SETTLING;
        }
        return settlingCalls == 1 ? Defect.NONE : Defect.MALFORMED;
    }

    /**
     * Completes the native {@code classify_turn -> tool result -> settling call} protocol without executing either
     * function. The pending result is deliberately not an execution acknowledgement: the thought receives the
     * completed pair and remains the sole owner of classification, memory, safety, and game side effects.
     */
    private LlmResult continueAfterClassification(LlmRequest request, Attempt classifyOnly, int attemptNumber) {
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        LlmToolInvocation classify = onlyInvocation(classifyOnly.result());
        if (classify == null || !isClassify(classify)) {
            return INVALID; // defensive: MISSING_SETTLING is defined as exactly one classify call
        }
        LlmToolInvocation replayedClassify = withReplayIds(List.of(classify), "gateway-classify-",
                usedToolCallIds(request.messages())).get(0);
        CompanionDiagnostics.debug(trace, "llm", "attempt#" + (attemptNumber - 1)
                + " classify-only -> settling continuation");
        LlmRequest continuation = classificationContinuation(request, replayedClassify);
        Attempt settling = attempt(continuation, attemptNumber, RoundExpectation.SETTLING_AFTER_CLASSIFY);
        if (settling.defect() != Defect.NONE) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#" + attemptNumber + " settling continuation "
                    + settling.defect() + " -> INVALID");
            log.warn("LLM settling continuation has defect {}; returning INVALID_RESPONSE", settling.defect());
            return INVALID;
        }
        LlmToolInvocation settlingCall = settling.result().toolInvocations().get(0);
        CompanionDiagnostics.debug(trace, "llm", "attempt#" + attemptNumber + " settling continuation ok");
        return new LlmResult(LlmResult.Status.OK, List.of(replayedClassify, settlingCall),
                settling.result().finishReason(), settling.result().droppedText());
    }

    private static LlmToolInvocation onlyInvocation(LlmResult result) {
        return result.toolInvocations().size() == 1 ? result.toolInvocations().get(0) : null;
    }

    private static boolean isClassify(LlmToolInvocation invocation) {
        return ClassifyTurnFunction.ID.equals(invocation.name());
    }

    /**
     * Builds the local, non-executing continuation for a classify-only response. The tool result truthfully says
     * that execution is pending; it exists only to let a provider issue the next native function call.
     */
    private static LlmRequest classificationContinuation(LlmRequest request, LlmToolInvocation classify) {
        List<LlmMessage> messages = new ArrayList<>(request.messages());
        messages.add(LlmMessage.assistantToolCalls(List.of(classify)));
        messages.add(LlmMessage.toolResult(classify.id(),
                "{\"status\":\"received\",\"execution\":\"pending\","
                        + "\"next\":\"call exactly one settling function\"}"));
        return new LlmRequest(request.requestId(), List.copyOf(messages), request.tools(), request.profile(), request.trace());
    }

    /**
     * Builds a native repair continuation for a response that omitted {@code classify_turn}. Replayed calls were
     * never executed, so their tool result is truthfully rejected.
     */
    private LlmRequest repair(LlmRequest request, Attempt failedAttempt) {
        if (!canContinue(failedAttempt)) {
            return request;
        }
        List<LlmToolInvocation> replayedCalls = withReplayIds(failedAttempt.result().toolInvocations(),
                "repair-rejected-call-", usedToolCallIds(request.messages()));
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

    /** Collects ids already present in the local provider transcript, including historic tool results defensively. */
    private static Set<String> usedToolCallIds(List<LlmMessage> messages) {
        Set<String> used = new HashSet<>();
        for (LlmMessage message : messages) {
            for (LlmToolInvocation call : message.toolCalls()) {
                addId(used, call.id());
            }
            addId(used, message.toolCallId());
        }
        return used;
    }

    private static void addId(Set<String> used, String id) {
        if (id != null && !id.isBlank()) {
            used.add(id);
        }
    }

    /** Assigns ids unique across the full local provider transcript, keeping every result linked. */
    private static List<LlmToolInvocation> withReplayIds(
            List<LlmToolInvocation> calls,
            String syntheticIdPrefix,
            Set<String> occupiedIds
    ) {
        List<LlmToolInvocation> replayed = new ArrayList<>(calls.size());
        Set<String> usedIds = new HashSet<>(occupiedIds);
        int nextSyntheticId = 1;
        for (LlmToolInvocation call : calls) {
            String id = call.id();
            if (id == null || id.isBlank() || !usedIds.add(id)) {
                do {
                    id = syntheticIdPrefix + nextSyntheticId++;
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
            case NONE, MISSING_SETTLING, MALFORMED -> throw new IllegalArgumentException("No tool continuation for " + defect);
        };
    }
}
