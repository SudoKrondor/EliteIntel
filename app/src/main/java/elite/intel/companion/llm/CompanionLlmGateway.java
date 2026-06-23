package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Provider-neutral {@link LlmGateway}: orchestrates render -> send -> parse via the injected
 * {@link LlmProviderAdapter} and {@link LlmTransport}, enforces the tool-call-only contract, and does a
 * single repair/retry before reporting {@link LlmResult.Status#INVALID_RESPONSE}. A response is valid
 * only when it is one or more tool-calls whose names were actually offered this turn.
 * <p>
 * Threading: requests run on a single-thread executor (consciousness is serialized); {@link #submit}
 * returns immediately with a future.
 */
public final class CompanionLlmGateway implements LlmGateway {

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

    private LlmResult process(LlmRequest request) {
        LlmResult first = attempt(request);
        if (isUsable(first, request)) {
            return first;
        }
        // Single repair/retry: nudge the model that only a function call is acceptable.
        LlmResult second = attempt(repair(request));
        return isUsable(second, request) ? second : INVALID;
    }

    private LlmResult attempt(LlmRequest request) {
        String body = adapter.buildRequestBody(request);
        JsonObject response = transport.send(body);
        return adapter.parse(response);
    }

    /** Valid = OK status, at least one tool-call, and every called tool was offered this turn. */
    private boolean isUsable(LlmResult result, LlmRequest request) {
        if (!result.isValid() || result.toolInvocations().isEmpty()) {
            return false;
        }
        Set<String> offered = request.tools().stream()
                .map(LlmToolDefinition::name)
                .collect(Collectors.toSet());
        return result.toolInvocations().stream()
                .map(LlmToolInvocation::name)
                .allMatch(offered::contains);
    }

    private LlmRequest repair(LlmRequest request) {
        List<LlmMessage> messages = new ArrayList<>(request.messages());
        messages.add(LlmMessage.of(LlmMessageRole.USER,
                "Your previous response was not a valid function call. Respond only by calling one of the provided functions."));
        return new LlmRequest(request.requestId(), messages, request.tools(), request.profile());
    }
}
