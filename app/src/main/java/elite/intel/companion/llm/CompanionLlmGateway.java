package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.companion.diag.CompanionDiagnostics;
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
 * Symmetrically, a classifying turn must also carry a settling call: a response that calls
 * {@code classify_turn} and nothing else has classified the turn but neither answered nor acted, so it
 * falls silent. That draws the same single repair/retry (a nudge to speak or act) and the same graceful
 * degradation - if the retry still adds nothing, the classify-only response is accepted (the turn settles
 * silently) rather than downgraded to INVALID.
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
     * What is wrong with a parsed response; drives the repair nudge and the fallback. A {@code fatal} defect
     * (only {@link #MALFORMED} - not offered tool-calls at all) is unusable and downgrades to INVALID; the
     * non-fatal defects ({@link #MISSING_CLASSIFY} - usable but unclassified, {@link #MISSING_SETTLING} -
     * classified but silent) still leave a response that settles the turn, so after one failed repair they are
     * accepted as-is. {@link #NONE} means no defect.
     */
    private enum Defect {
        NONE(false), MISSING_CLASSIFY(false), MISSING_SETTLING(false), MALFORMED(true);
        final boolean fatal;
        Defect(boolean fatal) {
            this.fatal = fatal;
        }
    }

    /** A single send/parse round paired with the defect it was found to have. */
    private record Attempt(LlmResult result, Defect defect) {}

    private LlmResult process(LlmRequest request) {
        Attempt first = attempt(request);
        if (first.defect() == Defect.NONE) {
            return first.result();
        }
        // One repair/retry with a defect-targeted nudge. Surface it on the diagnostics surface (attributed to the
        // owning thought's trace) so this second physical call - otherwise only an unattributed token line - is
        // visible as part of the round.
        String trace = request.trace() != null ? request.trace() : CompanionDiagnostics.SYSTEM;
        CompanionDiagnostics.debug(trace, "llm", "attempt#1 " + first.defect() + " -> retry");
        log.warn("LLM response has defect {} (status={}, tool-calls={}); repairing and retrying once",
                first.defect(), first.result().status(), first.result().toolInvocations().size());
        Attempt second = attempt(repair(request, first.defect()));

        // Prefer a clean retry; otherwise keep the best usable (non-fatal) response - a missing classify_turn
        // or a classify-only silent turn still settles the turn for the commander, better than the INVALID
        // service phrase. Prefer the original answer over the nudged retry. Only a fatal defect is unusable.
        if (second.defect() == Defect.NONE) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#2 ok");
            return second.result();
        }
        if (!first.defect().fatal) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#2 still " + second.defect() + "; kept original");
            log.warn("accepting original response despite {}", first.defect());
            return first.result();
        }
        if (!second.defect().fatal) {
            CompanionDiagnostics.debug(trace, "llm", "attempt#2 kept repaired despite " + second.defect());
            log.warn("accepting repaired response despite {}", second.defect());
            return second.result();
        }
        CompanionDiagnostics.debug(trace, "llm", "attempt#2 still MALFORMED -> INVALID");
        log.warn("LLM response still malformed after retry; returning INVALID_RESPONSE");
        return INVALID;
    }

    private Attempt attempt(LlmRequest request) {
        String body = adapter.buildRequestBody(request);
        JsonObject response = transport.send(body);
        LlmResult result = adapter.parse(response);
        return new Attempt(result, defectOf(result, request));
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
     * Merges a terse, defect-targeted format correction into the leading {@link LlmMessageRole#SYSTEM} message
     * and returns the amended request. The nudge remains system-level instruction, not USER: a USER-role nudge
     * reads to the model as a new commander turn, so a chatty model burns the retry reasoning <em>about</em> the
     * error instead of just re-emitting the call. It is merged into the first system message instead of appended
     * as another message because strict local chat templates often allow only one optional system turn at the
     * beginning.
     */
    private LlmRequest repair(LlmRequest request, Defect defect) {
        String nudge = switch (defect) {
            case MISSING_CLASSIFY -> "Format correction: also call 'classify_turn' (first), then the settling call. Tool calls only.";
            case MISSING_SETTLING -> "Format correction: after 'classify_turn' also act - call 'speak' or the matching function. Tool calls only.";
            default -> "Format correction: reply only with tool calls - 'classify_turn' plus a settling call ('speak' or an action). No prose.";
        };
        List<LlmMessage> messages = new ArrayList<>(request.messages());
        if (!messages.isEmpty() && messages.get(0).role() == LlmMessageRole.SYSTEM) {
            LlmMessage first = messages.get(0);
            messages.set(0, LlmMessage.of(LlmMessageRole.SYSTEM, first.content() + "\n\n" + nudge));
        } else {
            messages.add(0, LlmMessage.of(LlmMessageRole.SYSTEM, nudge));
        }
        return new LlmRequest(request.requestId(), messages, request.tools(), request.profile(), request.trace());
    }
}
