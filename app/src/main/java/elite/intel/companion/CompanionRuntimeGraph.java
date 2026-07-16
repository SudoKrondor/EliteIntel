package elite.intel.companion;

import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.input.BargeInController;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MidTermToLongTermConsolidator;
import elite.intel.companion.memory.OversizedMemoryCompressor;
import elite.intel.companion.memory.SessionMemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.speech.GenerationBoundSpeechGateway;
import elite.intel.companion.speech.SpeechGateway;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One complete companion runtime generation. All references visible through {@link CompanionRuntime} belong to
 * this immutable composition, while the graph owns the mutable lifecycle of its dispatcher, background memory
 * workers, execution gateway, LLM gateway, and generation-bound speech requests.
 * <p>
 * Closing is idempotent. The generation is fenced first, then every owned resource is closed even if an earlier
 * cleanup step fails. An already-started game action is governed by {@link ExecutionGateway}'s contract; its
 * result is cancelled for this generation, while the physical action is not force-interrupted.
 */
public final class CompanionRuntimeGraph implements AutoCloseable {

    private final CompanionRuntimeGeneration runtimeGeneration;
    private final LlmGateway llmGateway;
    private final SpeechGateway speechGateway;
    private final ExecutionGateway executionGateway;
    private final MemoryGateway memoryGateway;
    private final CompanionActionReducer actionReducer;
    private final CompanionState companionState;
    private final CompanionNarrator narrator;

    private final ThoughtDispatcher thoughtDispatcher;
    private final ConfirmationCoordinator confirmationCoordinator;
    private final BargeInController bargeInController;
    private final SessionMemoryGateway sessionMemoryGateway;
    private final MidTermToLongTermConsolidator memoryConsolidator;
    private final OversizedMemoryCompressor oversizedMemoryCompressor;
    private final GenerationBoundSpeechGateway generationBoundSpeechGateway;
    private final AtomicBoolean started;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Creates the production graph and transfers ownership of all supplied lifecycle components to it.
     * The graph is assembled but its dispatcher is not started until {@link #start()}.
     */
    CompanionRuntimeGraph(
            CompanionRuntimeGeneration runtimeGeneration,
            LlmGateway llmGateway,
            GenerationBoundSpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            SessionMemoryGateway memoryGateway,
            CompanionActionReducer actionReducer,
            CompanionState companionState,
            CompanionNarrator narrator,
            ThoughtDispatcher thoughtDispatcher,
            ConfirmationCoordinator confirmationCoordinator,
            BargeInController bargeInController,
            MidTermToLongTermConsolidator memoryConsolidator,
            OversizedMemoryCompressor oversizedMemoryCompressor
    ) {
        this(runtimeGeneration, llmGateway, speechGateway, executionGateway, memoryGateway, actionReducer,
                companionState, narrator, thoughtDispatcher, confirmationCoordinator, bargeInController,
                memoryGateway, memoryConsolidator, oversizedMemoryCompressor, speechGateway, false);
    }

    /** Test-only composition seam used by the test-source runtime installer. */
    CompanionRuntimeGraph(
            CompanionRuntimeGeneration runtimeGeneration,
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            MemoryGateway memoryGateway,
            CompanionActionReducer actionReducer,
            CompanionState companionState,
            CompanionNarrator narrator
    ) {
        this(runtimeGeneration, llmGateway, speechGateway, executionGateway, memoryGateway, actionReducer,
                companionState, narrator, null, null, null, null, null, null,
                speechGateway instanceof GenerationBoundSpeechGateway generationBoundSpeechGateway
                        ? generationBoundSpeechGateway
                        : null,
                true);
    }

    private CompanionRuntimeGraph(
            CompanionRuntimeGeneration runtimeGeneration,
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            MemoryGateway memoryGateway,
            CompanionActionReducer actionReducer,
            CompanionState companionState,
            CompanionNarrator narrator,
            ThoughtDispatcher thoughtDispatcher,
            ConfirmationCoordinator confirmationCoordinator,
            BargeInController bargeInController,
            SessionMemoryGateway sessionMemoryGateway,
            MidTermToLongTermConsolidator memoryConsolidator,
            OversizedMemoryCompressor oversizedMemoryCompressor,
            GenerationBoundSpeechGateway generationBoundSpeechGateway,
            boolean initiallyStarted
    ) {
        this.runtimeGeneration = Objects.requireNonNull(runtimeGeneration, "runtimeGeneration");
        this.llmGateway = Objects.requireNonNull(llmGateway, "llmGateway");
        this.speechGateway = Objects.requireNonNull(speechGateway, "speechGateway");
        this.executionGateway = Objects.requireNonNull(executionGateway, "executionGateway");
        this.memoryGateway = Objects.requireNonNull(memoryGateway, "memoryGateway");
        this.actionReducer = Objects.requireNonNull(actionReducer, "actionReducer");
        this.companionState = Objects.requireNonNull(companionState, "companionState");
        this.narrator = Objects.requireNonNull(narrator, "narrator");
        this.thoughtDispatcher = thoughtDispatcher;
        this.confirmationCoordinator = confirmationCoordinator;
        this.bargeInController = bargeInController;
        this.sessionMemoryGateway = sessionMemoryGateway;
        this.memoryConsolidator = memoryConsolidator;
        this.oversizedMemoryCompressor = oversizedMemoryCompressor;
        this.generationBoundSpeechGateway = generationBoundSpeechGateway;
        this.started = new AtomicBoolean(initiallyStarted);
    }

    /** Starts the graph's intake workers before the complete graph is atomically published. */
    public synchronized void start() {
        if (closed.get()) {
            throw new IllegalStateException("Cannot start a closed companion runtime graph");
        }
        if (started.get()) {
            return;
        }
        if (thoughtDispatcher != null) {
            thoughtDispatcher.start();
        }
        started.set(true);
    }

    public CompanionRuntimeGeneration runtimeGeneration() {
        return runtimeGeneration;
    }

    public LlmGateway llmGateway() {
        return llmGateway;
    }

    public SpeechGateway speechGateway() {
        return speechGateway;
    }

    public ExecutionGateway executionGateway() {
        return executionGateway;
    }

    public MemoryGateway memoryGateway() {
        return memoryGateway;
    }

    public CompanionActionReducer actionReducer() {
        return actionReducer;
    }

    public CompanionState companionState() {
        return companionState;
    }

    public CompanionNarrator narrator() {
        return narrator;
    }

    public ThoughtDispatcher thoughtDispatcher() {
        return thoughtDispatcher;
    }

    public ConfirmationCoordinator confirmationCoordinator() {
        return confirmationCoordinator;
    }

    public BargeInController bargeInController() {
        return bargeInController;
    }

    /** Whether shutdown has started for this graph. */
    public boolean isClosed() {
        return closed.get();
    }

    /** Whether every startable component completed startup and the graph is safe to publish. */
    public boolean isStarted() {
        return started.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        runtimeGeneration.deactivate();

        Throwable cleanupFailure = null;
        cleanupFailure = runCleanup(cleanupFailure, () -> {
            if (sessionMemoryGateway != null) {
                sessionMemoryGateway.setPendingConsolidationListener(null);
                sessionMemoryGateway.setOversizedMemoryListener(null);
            }
        });
        cleanupFailure = runCleanup(cleanupFailure, () -> {
            if (confirmationCoordinator != null) {
                confirmationCoordinator.cancel();
            }
        });
        cleanupFailure = runCleanup(cleanupFailure, () -> {
            if (generationBoundSpeechGateway != null) {
                generationBoundSpeechGateway.close();
            }
        });
        cleanupFailure = runCleanup(cleanupFailure, () -> {
            if (thoughtDispatcher != null) {
                thoughtDispatcher.interruptLiveThoughts();
                thoughtDispatcher.stop();
            }
        });
        cleanupFailure = runCleanup(cleanupFailure, () -> closeIfPresent(oversizedMemoryCompressor));
        cleanupFailure = runCleanup(cleanupFailure, () -> closeIfPresent(memoryConsolidator));
        cleanupFailure = runCleanup(cleanupFailure, executionGateway::close);
        cleanupFailure = runCleanup(cleanupFailure, llmGateway::close);

        rethrowCleanupFailure(cleanupFailure);
    }

    private static Throwable runCleanup(Throwable previousFailure, Runnable cleanup) {
        try {
            cleanup.run();
            return previousFailure;
        } catch (RuntimeException | Error failure) {
            if (previousFailure == null) {
                return failure;
            }
            previousFailure.addSuppressed(failure);
            return previousFailure;
        }
    }

    private static void closeIfPresent(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Exception impossibleCheckedFailure) {
            throw new IllegalStateException("Companion resource cleanup failed", impossibleCheckedFailure);
        }
    }

    private static void rethrowCleanupFailure(Throwable cleanupFailure) {
        if (cleanupFailure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        if (cleanupFailure instanceof Error error) {
            throw error;
        }
    }
}
