package elite.intel.companion.execution;

import com.google.gson.JsonObject;
import elite.intel.companion.CompanionRuntimeGeneration;
import elite.intel.companion.model.execution.ExecutionRequest;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime-owned execution boundary that rejects submissions after its generation stops and cancels every
 * still-owned result on close. Closing is forwarded to the underlying gateway; injected gateways that own no
 * resources inherit {@link ExecutionGateway}'s no-op close contract.
 */
public final class GenerationBoundExecutionGateway implements ExecutionGateway {

    private final ExecutionGateway delegate;
    private final CompanionRuntimeGeneration runtimeGeneration;
    private final Set<CompletableFuture<JsonObject>> ownedResults = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    /** Wraps a gateway whose submissions and close lifecycle belong to {@code runtimeGeneration}. */
    public GenerationBoundExecutionGateway(
            ExecutionGateway delegate,
            CompanionRuntimeGeneration runtimeGeneration
    ) {
        this.delegate = java.util.Objects.requireNonNull(delegate, "delegate");
        this.runtimeGeneration = java.util.Objects.requireNonNull(runtimeGeneration, "runtimeGeneration");
    }

    @Override
    public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
        if (!acceptsSubmissions()) {
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException("Companion execution generation is no longer active"));
        }
        AtomicReference<CompletableFuture<JsonObject>> submittedResult = new AtomicReference<>();
        boolean submitted = runtimeGeneration.runIfActive(
                () -> submittedResult.set(delegate.submit(request)));
        if (!submitted) {
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException("Companion execution generation is no longer active"));
        }
        CompletableFuture<JsonObject> result = java.util.Objects.requireNonNull(
                submittedResult.get(), "Execution gateway returned null instead of a completion");
        ownedResults.add(result);
        result.whenComplete((ignored, failure) -> ownedResults.remove(result));
        if (!acceptsSubmissions()) {
            result.cancel(true); // shutdown raced with delegate submission: discard this generation's result
        }
        return result;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ownedResults.forEach(result -> result.cancel(true));
        ownedResults.clear();
        delegate.close();
    }

    private boolean acceptsSubmissions() {
        return !closed.get() && runtimeGeneration.isActive();
    }
}
