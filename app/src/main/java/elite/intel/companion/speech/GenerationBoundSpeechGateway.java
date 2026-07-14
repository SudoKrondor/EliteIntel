package elite.intel.companion.speech;

import elite.intel.companion.CompanionRuntimeGeneration;
import elite.intel.companion.model.speech.SpeechRequest;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime-owned speech boundary. It prevents an expired generation from starting new speech and cancels every
 * completion still owned by that generation during shutdown. The delegate remains the owner of its playback
 * implementation; this boundary owns only the requests submitted through it.
 */
public final class GenerationBoundSpeechGateway implements SpeechGateway, AutoCloseable {

    private final SpeechGateway delegate;
    private final CompanionRuntimeGeneration runtimeGeneration;
    private final Set<CompletableFuture<Void>> ownedUtterances = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    /** Wraps speech submissions so their completions belong to {@code runtimeGeneration}. */
    public GenerationBoundSpeechGateway(
            SpeechGateway delegate,
            CompanionRuntimeGeneration runtimeGeneration
    ) {
        this.delegate = java.util.Objects.requireNonNull(delegate, "delegate");
        this.runtimeGeneration = java.util.Objects.requireNonNull(runtimeGeneration, "runtimeGeneration");
    }

    @Override
    public CompletableFuture<Void> submit(SpeechRequest request) {
        if (!acceptsSubmissions()) {
            return cancelledCompletion();
        }
        AtomicReference<CompletableFuture<Void>> submittedCompletion = new AtomicReference<>();
        boolean submitted = runtimeGeneration.runIfActive(
                () -> submittedCompletion.set(delegate.submit(request)));
        if (!submitted) {
            return cancelledCompletion();
        }
        CompletableFuture<Void> completion = java.util.Objects.requireNonNull(
                submittedCompletion.get(), "Speech gateway returned null instead of a completion");
        ownedUtterances.add(completion);
        completion.whenComplete((ignored, failure) -> ownedUtterances.remove(completion));
        if (!acceptsSubmissions()) {
            completion.cancel(true); // shutdown raced with playback admission
        }
        return completion;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ownedUtterances.forEach(completion -> completion.cancel(true));
        ownedUtterances.clear();
    }

    private boolean acceptsSubmissions() {
        return !closed.get() && runtimeGeneration.isActive();
    }

    private static CompletableFuture<Void> cancelledCompletion() {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        completion.cancel(false);
        return completion;
    }
}
