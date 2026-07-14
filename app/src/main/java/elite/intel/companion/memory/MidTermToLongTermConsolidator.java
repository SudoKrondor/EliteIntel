package elite.intel.companion.memory;

import elite.intel.companion.CompanionRuntimeGeneration;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.speech.SpeechGateway;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consolidates mid-term entries (evicted into a buffer) into an updated {@code long_term_summary} via an
 * LLM compression call (see COMPANION_ARCHITECTURE.md §3.7). It is the {@link MidTermEvictionListener}
 * wired into the {@code MemoryGateway} at subsystem start.
 * <p>
 * Threading: buffering is synchronized and cheap; each compression pass runs on a single background
 * executor, so passes never overlap (each reads the just-replaced summary) and never block the memory
 * write path. On failure the buffered batch is dropped (raw entries are lost), the existing summary is
 * kept, diagnostics are written, and the commander is notified via a {@link SpeechGateway} system
 * notification; the failure is not written into companion memory.
 */
public final class MidTermToLongTermConsolidator implements MidTermEvictionListener, AutoCloseable {

    private static final Logger log = LogManager.getLogger(MidTermToLongTermConsolidator.class);

    /** Fixed system-notification phrase (companion memory text is code-generated, not LLM-generated). */
    private static final String FAILURE_NOTICE = "Memory consolidation failed; some older memory was lost.";

    private final MemoryGateway memoryGateway;
    private final LlmGateway llmGateway;
    private final SpeechGateway speechGateway;
    private final Executor executor;
    private final CompanionRuntimeGeneration runtimeGeneration;
    private final CompressionPromptComposer promptComposer = new CompressionPromptComposer();
    private final AtomicBoolean closed = new AtomicBoolean();

    private final Object lock = new Object();
    private final List<MemoryEntry> buffer = new ArrayList<>();

    /** Production: a single-thread daemon executor serializes compression passes off the write path. */
    public MidTermToLongTermConsolidator(MemoryGateway memoryGateway, LlmGateway llmGateway, SpeechGateway speechGateway) {
        this(memoryGateway, llmGateway, speechGateway, new CompanionRuntimeGeneration());
    }

    /** Production lifecycle: binds every delayed completion to the graph generation that owns it. */
    public MidTermToLongTermConsolidator(
            MemoryGateway memoryGateway,
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            CompanionRuntimeGeneration runtimeGeneration
    ) {
        this(memoryGateway, llmGateway, speechGateway, runtimeGeneration, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-consolidator");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** Test seam: inject a synchronous executor. */
    MidTermToLongTermConsolidator(MemoryGateway memoryGateway, LlmGateway llmGateway, SpeechGateway speechGateway,
                                  Executor executor) {
        this(memoryGateway, llmGateway, speechGateway, new CompanionRuntimeGeneration(), executor);
    }

    /** Test seam: inject both a runtime generation and a controlled executor. */
    MidTermToLongTermConsolidator(MemoryGateway memoryGateway, LlmGateway llmGateway, SpeechGateway speechGateway,
                                  CompanionRuntimeGeneration runtimeGeneration, Executor executor) {
        this.memoryGateway = memoryGateway;
        this.llmGateway = llmGateway;
        this.speechGateway = speechGateway;
        this.runtimeGeneration = runtimeGeneration;
        this.executor = executor;
    }

    @Override
    public void onEvicted(MemoryEntry entry) {
        if (!acceptsWork()) {
            return;
        }
        // Importance routes the entry: MAX is pinned verbatim into long-term right away (never summarized here
        // and never invisible in the buffering window), LOW is dropped, and HIGH/NORMAL buffer for
        // summarization. "Verbatim" is within the entry size cap: an over-long line (MAX included) was already
        // gist-compressed at write time by the gateway (CompanionConfig.memoryEntryMaxChars), so it never
        // reaches here at full length.
        switch (entry.importance()) {
            case MAX -> {
                runtimeGeneration.runIfActive(() -> {
                    if (!closed.get()) {
                        memoryGateway.addLongTermPinned(entry);
                    }
                });
                return;
            }
            case LOW -> {
                return;
            }
            default -> { /* HIGH, NORMAL: fall through to buffering */ }
        }
        List<MemoryEntry> batch = null;
        synchronized (lock) {
            if (!acceptsWork()) {
                return;
            }
            buffer.add(entry);
            if (buffer.size() >= CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD) {
                batch = new ArrayList<>(buffer); // take the batch out; new evictions keep accumulating
                buffer.clear();
            }
        }
        if (batch != null) {
            List<MemoryEntry> snapshot = batch;
            try {
                executor.execute(() -> consolidate(snapshot));
            } catch (RejectedExecutionException rejected) {
                if (acceptsWork()) {
                    throw rejected;
                }
            }
        }
    }

    /** One compression pass: current summary + batch -> new summary (validated), then atomic replace. */
    private void consolidate(List<MemoryEntry> batch) {
        try {
            String summary = llmGateway.compressMidTermMemory(compressionRequest(memoryGateway.longTermSummary(), batch)).get();
            if (!acceptsWork()) {
                return;
            }
            if (summary == null || summary.isBlank() || summary.length() > CompanionMemoryLimits.SUMMARY_MAX_CHARS) {
                fail("compression produced empty or oversized output (" + (summary == null ? "null" : summary.length() + " chars") + ")");
                return;
            }
            String replacementSummary = summary.strip();
            runtimeGeneration.runIfActive(() -> {
                if (!closed.get()) {
                    memoryGateway.replaceLongTermSummary(replacementSummary);
                }
            });
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            if (acceptsWork()) {
                fail("compression call interrupted");
            }
        } catch (Exception failure) { // provider error; the batch is already lost
            if (acceptsWork()) {
                fail("compression call failed: " + failure.getMessage());
            }
        }
    }

    private void fail(String reason) {
        if (!acceptsWork()) {
            return;
        }
        // Existing summary, short-term and remaining mid-term are untouched; only this batch is lost.
        log.warn("Mid-term consolidation failed, batch discarded: {}", reason);
        runtimeGeneration.runIfActive(() -> {
            if (!closed.get()) {
                speechGateway.submit(new SpeechRequest(UUID.randomUUID().toString(), FAILURE_NOTICE, Urgency.NORMAL));
            }
        });
    }

    /** Stops accepting evictions, drops the unsubmitted buffer, and interrupts the owned worker if present. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (lock) {
            buffer.clear();
        }
        if (executor instanceof ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }

    private boolean acceptsWork() {
        return !closed.get() && runtimeGeneration.isActive();
    }

    /** Wraps the composer's compression messages into a no-tools COMPRESSION request. */
    private LlmRequest compressionRequest(String currentSummary, List<MemoryEntry> batch) {
        return new LlmRequest(UUID.randomUUID().toString(),
                promptComposer.compose(currentSummary, batch), List.of(), PromptCacheProfile.COMPRESSION);
    }
}
