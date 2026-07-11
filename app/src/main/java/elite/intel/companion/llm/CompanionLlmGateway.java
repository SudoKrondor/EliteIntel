package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.tools.ClassifyTurnFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Provider-neutral {@link LlmGateway}: orchestrates render -> send -> parse via the injected
 * {@link LlmProviderAdapter} and {@link LlmTransport}, enforces the tool-call-only contract, and does a
 * single repair/retry before reporting {@link LlmResult.Status#INVALID_RESPONSE}. A response is valid
 * only when it is one or more tool-calls whose names were actually offered this turn.
 * <p>
 * It also enforces the classify-first protocol: when {@code classify_turn} is among the offered tools (a
 * classifying turn - narration never offers it), the completed logical result must contain exactly one
 * {@code classify_turn} and exactly one settling call.
 * <p>
 * A provider may split either side of that pair into a second native tool-call round. A response containing only
 * {@code classify_turn}, or only one valid offered settling call, is therefore not repaired or executed. The
 * gateway replays that call with a truthful pending result, requests only the missing call, and returns the
 * expected classify-first pair to the thought. Other incomplete responses retain the single repair/retry. Every
 * local protocol transcript is request-local and is never written to durable memory.
 * <p>
 * Threading: requests run on a single-thread executor (consciousness is serialized); {@link #submit}
 * returns immediately with a future. Cancelling that future interrupts the exact executor task and, through
 * the provider client's interruptible HTTP wait, cancels the physical exchange. One logical deadline covers
 * queue wait plus every repair/continuation attempt, so a stalled call cannot retain the sole worker beyond
 * the thought watchdog.
 */
public final class CompanionLlmGateway implements LlmGateway {

    private static final Logger log = LogManager.getLogger(CompanionLlmGateway.class);

    private static final LlmResult INVALID = new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    private final LlmProviderAdapter adapter;
    private final LlmTransport transport;
    private final Executor executor;
    private final Duration logicalDeadline;

    public CompanionLlmGateway(LlmProviderAdapter adapter, LlmTransport transport) {
        this(adapter, transport, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-llm");
            thread.setDaemon(true);
            return thread;
        }), CompanionConfig.llmLogicalDeadline());
    }

    /** Test seam: inject a synchronous/controlled executor. */
    CompanionLlmGateway(LlmProviderAdapter adapter, LlmTransport transport, Executor executor) {
        this(adapter, transport, executor, CompanionConfig.llmLogicalDeadline());
    }

    /** Test seam: inject the executor and the total deadline shared by every physical attempt. */
    CompanionLlmGateway(
            LlmProviderAdapter adapter,
            LlmTransport transport,
            Executor executor,
            Duration logicalDeadline
    ) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.logicalDeadline = Objects.requireNonNull(logicalDeadline, "logicalDeadline");
        if (logicalDeadline.isZero() || logicalDeadline.isNegative()) {
            throw new IllegalArgumentException("logicalDeadline must be positive");
        }
    }

    @Override
    public CompletableFuture<LlmResult> submit(LlmRequest request) {
        return submitCancellable(request, () -> process(request));
    }

    @Override
    public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
        // Plain-text turn (request carries no tools): render -> send -> extract text; null on bad output.
        return submitCancellable(request, () -> {
            ensureActive();
            String body = adapter.buildRequestBody(request);
            ensureActive();
            JsonObject response = transport.send(body);
            ensureActive();
            return adapter.parseText(response);
        });
    }

    /**
     * Runs one logical request on an explicitly cancellable task. {@link CompletableFuture#cancel(boolean)} does
     * not interrupt a {@code supplyAsync} supplier by itself, so the returned future is deliberately bridged to
     * the underlying {@link FutureTask}. The one timeout is armed before queueing and therefore covers queue wait,
     * initial send, repair, and classify continuation together.
     */
    private <T> CompletableFuture<T> submitCancellable(LlmRequest request, Supplier<T> operation) {
        CompletableFuture<T> result = new CompletableFuture<>();
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        FutureTask<Void> task = new FutureTask<>(() -> {
            if (result.isDone()) {
                return null;
            }
            try {
                ensureActive();
                T value = operation.get();
                ensureActive();
                result.complete(value);
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
            return null;
        });

        result.orTimeout(logicalDeadline.toMillis(), TimeUnit.MILLISECONDS);
        result.whenComplete((ignored, failure) -> {
            if (result.isCancelled()) {
                task.cancel(true);
                CompanionDiagnostics.debug(trace, "llm", "request cancelled; physical call interrupted");
            } else if (isTimeout(failure)) {
                task.cancel(true);
                CompanionDiagnostics.debug(trace, "llm", "logical deadline "
                        + logicalDeadline.toMillis() + " ms exceeded; physical call interrupted");
            }
        });

        try {
            executor.execute(task);
        } catch (RuntimeException rejected) {
            result.completeExceptionally(rejected);
        }
        return result;
    }

    private static boolean isTimeout(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /** Stops parse/retry work after the owning future has interrupted this exact task. */
    private static void ensureActive() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("LLM request interrupted");
        }
    }

    /**
     * Shuts down the owned executor. The companion runtime graph calls this during stop/restart; callers that
     * build a throwaway gateway (e.g. custom command key generation) close it after their blocking call returns.
     * The injected-executor test seam passes a non-{@link ExecutorService} executor, for which this is a no-op.
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
        SETTLING_AFTER_CLASSIFY,
        CLASSIFY_AFTER_SETTLING
    }

    /** A single send/parse round paired with the defect it was found to have. */
    private record Attempt(LlmResult result, Defect defect) {}

    private LlmResult process(LlmRequest request) {
        ensureActive();
        Attempt firstAttempt = attempt(request, 1, RoundExpectation.INITIAL);
        if (firstAttempt.defect() == Defect.NONE) {
            return firstAttempt.result();
        }
        if (firstAttempt.defect() == Defect.MISSING_SETTLING) {
            return continueAfterClassification(request, firstAttempt, 2);
        }
        if (isSettlingOnly(firstAttempt)) {
            return continueAfterSettlingCall(request, firstAttempt, 2);
        }
        // One protocol-valid repair/retry. Surface it on the diagnostics surface (attributed to the owning
        // thought's trace) so this second physical call is visible as part of the round.
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        String retryKind = canBuildRejectedContinuation(firstAttempt)
                ? "assistant/tool continuation"
                : "unchanged request";
        CompanionDiagnostics.debug(trace, "llm", "attempt#1 " + firstAttempt.defect()
                + " calls=" + CompanionDiagnostics.calls(firstAttempt.result().toolInvocations())
                + " -> retry (" + retryKind + ")");
        log.warn("LLM response has defect {} (status={}, tool-calls={}); retrying once via {}",
                firstAttempt.defect(), firstAttempt.result().status(),
                firstAttempt.result().toolInvocations().size(), retryKind);
        LlmRequest repairedRequest = buildRepairRequest(request, firstAttempt);
        Attempt secondAttempt = attempt(repairedRequest, 2, RoundExpectation.INITIAL);

        if (secondAttempt.defect() == Defect.NONE) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#2 ok");
            return secondAttempt.result();
        }
        if (secondAttempt.defect() == Defect.MISSING_SETTLING) {
            return continueAfterClassification(repairedRequest, secondAttempt, 3);
        }
        if (isSettlingOnly(secondAttempt)) {
            return continueAfterSettlingCall(repairedRequest, secondAttempt, 3);
        }
        CompanionDiagnostics.debug(trace, "llm", "attempt#2 still " + secondAttempt.defect()
                + " calls=" + CompanionDiagnostics.calls(secondAttempt.result().toolInvocations()) + " -> INVALID");
        log.warn("LLM response still has defect {} after retry; returning INVALID_RESPONSE", secondAttempt.defect());
        return INVALID;
    }

    /** Runs one physical provider request and validates it for the expected position in the local tool flow. */
    private Attempt attempt(LlmRequest request, int attemptNumber, RoundExpectation expectation) {
        long attemptStartedNanos = System.nanoTime();
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        try {
            ensureActive();
            long renderStartedNanos = System.nanoTime();
            String body = adapter.buildRequestBody(request);
            ensureActive();
            long renderMillis = elapsedMillis(renderStartedNanos);
            long httpStartedNanos = System.nanoTime();
            JsonObject response = transport.send(body);
            ensureActive();
            long httpMillis = elapsedMillis(httpStartedNanos);
            long parseStartedNanos = System.nanoTime();
            LlmResult result = adapter.parse(response);
            ensureActive();
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
     * classifying turn contains exactly one classify and exactly one settling call. A one-sided follow-up must
     * contain exactly the one missing offered call.
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
        if (expectation == RoundExpectation.CLASSIFY_AFTER_SETTLING) {
            return result.toolInvocations().size() == 1 && isClassify(result.toolInvocations().get(0))
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
    private LlmResult continueAfterClassification(
            LlmRequest request,
            Attempt classifyOnlyAttempt,
            int attemptNumber
    ) {
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        LlmToolInvocation classifyCall = onlyInvocation(classifyOnlyAttempt.result());
        if (classifyCall == null || !isClassify(classifyCall)) {
            return INVALID; // defensive: MISSING_SETTLING is defined as exactly one classify call
        }
        LlmToolInvocation replayedClassifyCall = withReplayIds(List.of(classifyCall), "gateway-classify-",
                usedToolCallIds(request.messages())).get(0);
        CompanionDiagnostics.debug(trace, "llm", "attempt#" + (attemptNumber - 1)
                + " classify-only -> settling continuation: "
                + CompanionDiagnostics.calls(List.of(replayedClassifyCall)));
        LlmRequest continuationRequest = buildSettlingContinuationRequest(request, replayedClassifyCall);
        Attempt settlingAttempt = attempt(
                continuationRequest, attemptNumber, RoundExpectation.SETTLING_AFTER_CLASSIFY);
        if (settlingAttempt.defect() != Defect.NONE) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#" + attemptNumber + " settling continuation "
                    + settlingAttempt.defect() + " calls="
                    + CompanionDiagnostics.calls(settlingAttempt.result().toolInvocations()) + " -> INVALID");
            log.warn("LLM settling continuation has defect {}; returning INVALID_RESPONSE", settlingAttempt.defect());
            return INVALID;
        }
        LlmToolInvocation settlingCall = settlingAttempt.result().toolInvocations().get(0);
        CompanionDiagnostics.debug(trace, "llm", "attempt#" + attemptNumber + " settling continuation ok: "
                + CompanionDiagnostics.calls(List.of(settlingCall)));
        return new LlmResult(LlmResult.Status.OK, List.of(replayedClassifyCall, settlingCall),
                settlingAttempt.result().finishReason(), settlingAttempt.result().droppedText());
    }

    /**
     * Completes a provider-deviant {@code settling call -> tool result -> classify_turn} exchange without executing
     * either function. The returned result is normalized to the classify-first order expected by the thought.
     */
    private LlmResult continueAfterSettlingCall(
            LlmRequest request,
            Attempt settlingOnlyAttempt,
            int attemptNumber
    ) {
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        LlmToolInvocation settlingCall = onlyInvocation(settlingOnlyAttempt.result());
        if (settlingCall == null || isClassify(settlingCall)) {
            return INVALID; // defensive: settling-only means exactly one non-classify offered call
        }
        LlmToolInvocation replayedSettlingCall = withReplayIds(List.of(settlingCall), "gateway-settling-",
                usedToolCallIds(request.messages())).get(0);
        CompanionDiagnostics.debug(trace, "llm", "attempt#" + (attemptNumber - 1)
                + " settling-only -> classify continuation: "
                + CompanionDiagnostics.calls(List.of(replayedSettlingCall)));
        LlmRequest continuationRequest = buildClassificationContinuationRequest(request, replayedSettlingCall);
        Attempt classificationAttempt = attempt(
                continuationRequest, attemptNumber, RoundExpectation.CLASSIFY_AFTER_SETTLING);
        if (classificationAttempt.defect() != Defect.NONE) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#" + attemptNumber + " classify continuation "
                    + classificationAttempt.defect() + " calls="
                    + CompanionDiagnostics.calls(classificationAttempt.result().toolInvocations()) + " -> INVALID");
            log.warn("LLM classify continuation has defect {}; returning INVALID_RESPONSE",
                    classificationAttempt.defect());
            return INVALID;
        }
        LlmToolInvocation classifyCall = classificationAttempt.result().toolInvocations().get(0);
        CompanionDiagnostics.debug(trace, "llm", "attempt#" + attemptNumber + " classify continuation ok: "
                + CompanionDiagnostics.calls(List.of(classifyCall)));
        return new LlmResult(LlmResult.Status.OK, List.of(classifyCall, replayedSettlingCall),
                classificationAttempt.result().finishReason(), classificationAttempt.result().droppedText());
    }

    private static boolean isSettlingOnly(Attempt attempt) {
        LlmToolInvocation invocation = onlyInvocation(attempt.result());
        return attempt.defect() == Defect.MISSING_CLASSIFY && invocation != null && !isClassify(invocation);
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
    private static LlmRequest buildSettlingContinuationRequest(
            LlmRequest request,
            LlmToolInvocation classifyCall
    ) {
        List<LlmMessage> messages = new ArrayList<>(request.messages());
        messages.add(LlmMessage.assistantToolCalls(List.of(classifyCall)));
        messages.add(LlmMessage.toolResult(classifyCall.id(),
                "{\"status\":\"received\",\"execution\":\"pending\","
                        + "\"next\":\"call exactly one settling function\"}"));
        return new LlmRequest(request.requestId(), List.copyOf(messages), request.tools(), request.profile(), request.trace());
    }

    /**
     * Builds the local, non-executing continuation for a settling-only response. The tool result keeps the offered
     * action pending while asking the provider for only the missing classification metadata.
     */
    private static LlmRequest buildClassificationContinuationRequest(
            LlmRequest request,
            LlmToolInvocation settlingCall
    ) {
        List<LlmMessage> messages = new ArrayList<>(request.messages());
        messages.add(LlmMessage.assistantToolCalls(List.of(settlingCall)));
        messages.add(LlmMessage.toolResult(settlingCall.id(),
                "{\"status\":\"received\",\"execution\":\"pending\","
                        + "\"next\":\"call classify_turn only\"}"));
        return new LlmRequest(request.requestId(), List.copyOf(messages), request.tools(), request.profile(), request.trace());
    }

    /**
     * Builds a native repair continuation for a response that omitted {@code classify_turn}. Replayed calls were
     * never executed, so their tool result is truthfully rejected.
     */
    private LlmRequest buildRepairRequest(LlmRequest request, Attempt failedAttempt) {
        if (!canBuildRejectedContinuation(failedAttempt)) {
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

    private static boolean canBuildRejectedContinuation(Attempt failedAttempt) {
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
