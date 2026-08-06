package elite.intel.ai.brain.vega.llm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.AiTransportResult;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.vega.CompanionConfig;
import elite.intel.ai.brain.vega.diag.CompanionDiagnostics;
import elite.intel.ai.brain.vega.model.llm.*;
import elite.intel.ai.brain.vega.tools.RequestInputFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Provider-neutral {@link LlmGateway}: orchestrates render -> send -> parse via the injected
 * {@link LlmProviderAdapter} and {@link LlmTransport}, enforces the tool-call-only contract, and does one
 * protocol repair for an invalid model response before reporting {@link LlmResult.Status#INVALID_RESPONSE}.
 * A transient transport failure gets one jittered resend, while a permanent transport failure skips protocol
 * repair. Every valid response contains exactly one offered function call whose arguments match its exact schema.
 * A malformed, multi-call or schema-invalid response
 * receives one protocol repair before the gateway reports it as invalid.
 * <p>
 * Threading: requests run on a single-thread executor (consciousness is serialized); {@link #submit}
 * returns immediately with a future. Cancelling that future interrupts the exact executor task and, through
 * the provider client's interruptible HTTP wait, cancels the physical exchange. One logical deadline covers
 * queue wait plus the repair and bounded transport resend, so a stalled call cannot retain the
 * sole worker beyond the thought watchdog.
 */
public final class CompanionLlmGateway implements LlmGateway {

    private static final Logger log = LogManager.getLogger(CompanionLlmGateway.class);

    private static final LlmResult INVALID = new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    private static final long TRANSIENT_RETRY_MIN_DELAY_MILLIS = 250;
    private static final long TRANSIENT_RETRY_MAX_DELAY_MILLIS = 750;
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
            AiTransportResult outcome = sendTransportWithTransientRetry(request, body, 1);
            ensureActive();
            if (outcome instanceof AiTransportResult.Success success) {
                return adapter.parseText(success.response());
            }
            return null;
        });
    }

    /**
     * Runs one logical request on an explicitly cancellable task. {@link CompletableFuture#cancel(boolean)} does
     * not interrupt a {@code supplyAsync} supplier by itself, so the returned future is deliberately bridged to
     * the underlying {@link FutureTask}. The one timeout is armed before queueing and therefore covers queue wait,
     * initial send and repair together.
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
        NONE, INVALID_TOOL_CALL, MALFORMED,
        TRANSIENT_TRANSPORT, PERMANENT_TRANSPORT, CANCELLED_TRANSPORT
    }

    /** A single send/parse round paired with its protocol defect or typed transport failure. */
    private record Attempt(LlmResult result, Defect defect, AiTransportResult.Failure transportFailure) {
        private boolean hasTransportFailure() {
            return transportFailure != null;
        }
    }

    private LlmResult process(LlmRequest request) {
        ensureActive();
        Attempt firstAttempt = attempt(request, 1);
        if (firstAttempt.hasTransportFailure()) {
            return transportFailureResult(request, firstAttempt);
        }
        if (firstAttempt.defect() == Defect.NONE) {
            return firstAttempt.result();
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
        Attempt secondAttempt = attempt(repairedRequest, 2);

        if (secondAttempt.hasTransportFailure()) {
            return transportFailureResult(repairedRequest, secondAttempt);
        }

        if (secondAttempt.defect() == Defect.NONE) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#2 ok");
            return secondAttempt.result();
        }
        CompanionDiagnostics.debug(trace, "llm", "attempt#2 still " + secondAttempt.defect()
                + " calls=" + CompanionDiagnostics.calls(secondAttempt.result().toolInvocations()) + " -> INVALID");
        log.warn("LLM response still has defect {} after retry; returning INVALID_RESPONSE", secondAttempt.defect());
        return INVALID;
    }

    /** Runs one logical provider attempt and validates it for the expected position in the local tool flow. */
    private Attempt attempt(LlmRequest request, int attemptNumber) {
        long attemptStartedNanos = System.nanoTime();
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        try {
            ensureActive();
            long renderStartedNanos = System.nanoTime();
            String body = adapter.buildRequestBody(request);
            ensureActive();
            long renderMillis = elapsedMillis(renderStartedNanos);
            long httpStartedNanos = System.nanoTime();
            AiTransportResult outcome = sendTransportWithTransientRetry(request, body, attemptNumber);
            ensureActive();
            long httpMillis = elapsedMillis(httpStartedNanos);
            if (outcome instanceof AiTransportResult.Failure failure) {
                if (failure.kind() == AiTransportResult.FailureKind.MALFORMED_RESPONSE) {
                    return new Attempt(INVALID, Defect.MALFORMED, null);
                }
                return new Attempt(INVALID, transportDefect(failure), failure);
            }
            JsonObject response = ((AiTransportResult.Success) outcome).response();
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
            return new Attempt(result, defectOf(result, request), null);
        } catch (RuntimeException failure) {
            CompanionDiagnostics.debug(trace, "llm-http",
                    "attempt=" + attemptNumber + " failed after " + elapsedMillis(attemptStartedNanos) + " ms");
            throw failure;
        }
    }

    /** Runs one delayed retry only for a typed transient transport failure, within the owning logical deadline. */
    private AiTransportResult sendTransportWithTransientRetry(LlmRequest request, String body, int attemptNumber) {
        AiTransportResult firstOutcome = transport.sendOutcome(body);
        if (!(firstOutcome instanceof AiTransportResult.Failure failure)
                || failure.kind() != AiTransportResult.FailureKind.TRANSIENT) {
            return firstOutcome;
        }
        long delayMillis = ThreadLocalRandom.current().nextLong(
                TRANSIENT_RETRY_MIN_DELAY_MILLIS, TRANSIENT_RETRY_MAX_DELAY_MILLIS + 1);
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        CompanionDiagnostics.debug(trace, "llm-http", "attempt=" + attemptNumber + " transient transport"
                + transportStatus(failure) + " -> retry in " + delayMillis + " ms");
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new CancellationException("LLM request interrupted during transport retry delay");
        }
        ensureActive();
        return transport.sendOutcome(body);
    }

    /** Turns a non-parseable transport outcome into the gateway's terminal protocol result. */
    private LlmResult transportFailureResult(LlmRequest request, Attempt attempt) {
        AiTransportResult.Failure failure = attempt.transportFailure();
        if (failure.kind() == AiTransportResult.FailureKind.CANCELLED) {
            throw new CancellationException("LLM transport request was cancelled");
        }
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        CompanionDiagnostics.debug(trace, "llm-http", "transport " + failure.kind()
                + transportStatus(failure) + " -> INVALID without protocol repair");
        log.warn("LLM transport failed with {}{}; returning INVALID_RESPONSE without protocol repair",
                failure.kind(), transportStatus(failure));
        return INVALID;
    }

    private static Defect transportDefect(AiTransportResult.Failure failure) {
        return switch (failure.kind()) {
            case TRANSIENT -> Defect.TRANSIENT_TRANSPORT;
            case PERMANENT -> Defect.PERMANENT_TRANSPORT;
            case CANCELLED -> Defect.CANCELLED_TRANSPORT;
            case MALFORMED_RESPONSE -> Defect.MALFORMED;
        };
    }

    private static String transportStatus(AiTransportResult.Failure failure) {
        return failure.statusCode() == null ? "" : " HTTP " + failure.statusCode();
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    /** Validates that the model returned exactly one offered call with its exact schema. */
    private Defect defectOf(LlmResult result, LlmRequest request) {
        if (!result.isValid() || result.toolInvocations().size() != 1) {
            return Defect.MALFORMED;
        }
        return ToolCallValidator.validateAndNormalizeExactSchemas(result.toolInvocations(), request.tools())
                ? Defect.NONE : Defect.INVALID_TOOL_CALL;
    }

    /**
     * Builds a native repair continuation for a parsed response that could not be executed. Replayed calls were
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
        for (LlmToolInvocation call : replayedCalls) {
            messages.add(LlmMessage.toolResult(call.id(),
                    rejectionFor(failedAttempt.defect(), call, request.tools())));
        }
        return new LlmRequest(request.requestId(), List.copyOf(messages),
                repairTools(request.tools(), failedAttempt), request.profile(), request.trace());
    }

    /**
     * Once the model selected an offered function, a schema error is repaired against that function alone. The
     * semantic choice was valid; keeping unrelated functions (especially {@code speak}) available lets the repair
     * silently abandon the original request instead of correcting its arguments.
     */
    private static List<LlmToolDefinition> repairTools(
            List<LlmToolDefinition> offeredTools,
            Attempt failedAttempt
    ) {
        if (failedAttempt.defect() != Defect.INVALID_TOOL_CALL
                || failedAttempt.result().toolInvocations().size() != 1) {
            return offeredTools;
        }
        LlmToolInvocation call = failedAttempt.result().toolInvocations().get(0);
        return unsatisfiableInputRequestTarget(call, offeredTools)
                .or(() -> offeredTools.stream()
                        .filter(tool -> Objects.equals(tool.name(), call.name()))
                        .findFirst())
                .map(List::<LlmToolDefinition>of)
                .orElse(offeredTools);
    }

    /**
     * The listed function a rejected {@code request_input} was really aiming at, when that function declares no
     * required parameter and so cannot be missing one. Asking for input is then provably wrong - whether
     * {@code request_input} was offered at all (it is withheld when no offered function takes a required
     * argument) or merely mis-shaped - and the model named its choice in {@code action_id}. Narrowing the repair
     * to that function keeps the commander's order executable instead of letting the retry fall back on
     * {@code speak}, which answers an order with conversation.
     */
    private static Optional<LlmToolDefinition> unsatisfiableInputRequestTarget(
            LlmToolInvocation call,
            List<LlmToolDefinition> offeredTools
    ) {
        if (!RequestInputFunction.ID.equals(call.name()) || call.arguments() == null) {
            return Optional.empty();
        }
        JsonElement actionId = call.arguments().get(RequestInputFunction.PARAM_ACTION_ID);
        if (actionId == null || !actionId.isJsonPrimitive() || !actionId.getAsJsonPrimitive().isString()) {
            return Optional.empty();
        }
        return offeredTools.stream()
                .filter(tool -> Objects.equals(tool.name(), actionId.getAsString()))
                .filter(tool -> tool.parameters().stream().noneMatch(ActionParameterSpec::isRequired))
                .findFirst();
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
    private static String rejectionFor(
            Defect defect,
            LlmToolInvocation call,
            List<LlmToolDefinition> offeredTools
    ) {
        return switch (defect) {
            case INVALID_TOOL_CALL -> rejectionPayload(invalidToolCallReason(call, offeredTools));
            case NONE, MALFORMED, TRANSIENT_TRANSPORT, PERMANENT_TRANSPORT, CANCELLED_TRANSPORT ->
                    throw new IllegalArgumentException("No tool continuation for " + defect);
        };
    }

    /** Gives the model the exact correction for the rejected call without weakening schema validation. */
    private static String invalidToolCallReason(
            LlmToolInvocation call,
            List<LlmToolDefinition> offeredTools
    ) {
        LlmToolDefinition offered = offeredTools.stream()
                .filter(tool -> Objects.equals(tool.name(), call.name()))
                .findFirst()
                .orElse(null);
        LlmToolDefinition inputRequestTarget = unsatisfiableInputRequestTarget(call, offeredTools).orElse(null);
        String correction;
        if (inputRequestTarget != null) {
            correction = "The listed function " + inputRequestTarget.name() + " declares no required parameter, so "
                    + "nothing is missing and " + call.name() + " does not apply. Call "
                    + inputRequestTarget.name() + " itself to carry out the original request.";
        } else if (offered == null) {
            correction = "The function " + call.name() + " is not listed; choose only from the listed functions.";
        } else if (offered.parameters().isEmpty()) {
            correction = "The listed function " + offered.name() + " accepts no arguments. If it fulfills the "
                    + "original request, retry " + offered.name() + " with {}. Do not add argument fields; words "
                    + "from the original request are arguments only when its schema declares them.";
        } else {
            String parameterNames = offered.parameters().stream()
                    .map(parameter -> parameter.getName())
                    .collect(Collectors.joining(", "));
            correction = "The listed function " + offered.name() + " accepts only these argument fields: "
                    + parameterNames + ". Use their exact declared types.";
        }
        return "No call was executed. " + correction + " The functions listed in this request remain available. "
                + "Re-read the original request and retry with exactly one listed function using only declared "
                + "parameters. Do not call speak merely because this attempt was rejected; call speak only "
                + "when none of the other listed functions can fulfill the original request.";
    }

    private static String rejectionPayload(String reason) {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", "rejected");
        payload.addProperty("reason", reason);
        return payload.toString();
    }
}
