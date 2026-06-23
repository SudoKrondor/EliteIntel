package elite.intel.companion.memory;

import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.speech.SpeechGateway;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
public final class MidTermToLongTermConsolidator implements MidTermEvictionListener {

    private static final Logger log = LogManager.getLogger(MidTermToLongTermConsolidator.class);

    /** Fixed system-notification phrase (companion memory text is code-generated, not LLM-generated). */
    private static final String FAILURE_NOTICE = "Memory consolidation failed; some older memory was lost.";

    private final MemoryGateway memoryGateway;
    private final LlmGateway llmGateway;
    private final SpeechGateway speechGateway;
    private final Executor executor;

    private final Object lock = new Object();
    private final List<MemoryEntry> buffer = new ArrayList<>();

    /** Production: a single-thread daemon executor serializes compression passes off the write path. */
    public MidTermToLongTermConsolidator(MemoryGateway memoryGateway, LlmGateway llmGateway, SpeechGateway speechGateway) {
        this(memoryGateway, llmGateway, speechGateway, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-consolidator");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** Test seam: inject a synchronous executor. */
    MidTermToLongTermConsolidator(MemoryGateway memoryGateway, LlmGateway llmGateway, SpeechGateway speechGateway,
                                  Executor executor) {
        this.memoryGateway = memoryGateway;
        this.llmGateway = llmGateway;
        this.speechGateway = speechGateway;
        this.executor = executor;
    }

    @Override
    public void onEvicted(MemoryEntry entry) {
        List<MemoryEntry> batch = null;
        synchronized (lock) {
            buffer.add(entry);
            if (buffer.size() >= CompanionMemoryLimits.CONSOLIDATION_BUFFER_THRESHOLD) {
                batch = new ArrayList<>(buffer); // take the batch out; new evictions keep accumulating
                buffer.clear();
            }
        }
        if (batch != null) {
            List<MemoryEntry> snapshot = batch;
            executor.execute(() -> consolidate(snapshot));
        }
    }

    /** One compression pass: current summary + batch -> new summary (validated), then atomic replace. */
    private void consolidate(List<MemoryEntry> batch) {
        try {
            String summary = llmGateway.compressMidTermMemory(compressionRequest(memoryGateway.longTermSummary(), batch)).get();
            if (summary == null || summary.isBlank() || summary.length() > CompanionMemoryLimits.SUMMARY_MAX_CHARS) {
                fail("compression produced empty or oversized output (" + (summary == null ? "null" : summary.length() + " chars") + ")");
                return;
            }
            memoryGateway.replaceLongTermSummary(summary.strip());
        } catch (Exception e) { // includes interruption / provider errors; the batch is already lost
            fail("compression call failed: " + e.getMessage());
        }
    }

    private void fail(String reason) {
        // Existing summary, short-term, remaining mid-term and llm_memory are untouched; only this batch is lost.
        log.warn("Mid-term consolidation failed, batch discarded: {}", reason);
        speechGateway.submit(new SpeechRequest(UUID.randomUUID().toString(), FAILURE_NOTICE, Urgency.NORMAL));
    }

    /** Builds the compression-mode request: English instruction + current summary + buffered entries, no tools. */
    private LlmRequest compressionRequest(String currentSummary, List<MemoryEntry> batch) {
        String instruction = "You compress old crew memory into a single compact running summary. "
                + "Merge the new entries into the existing summary, keep what stays relevant, drop trivia, "
                + "and reply with only the updated summary as plain text, at most "
                + CompanionMemoryLimits.SUMMARY_MAX_CHARS + " characters.";
        StringBuilder user = new StringBuilder();
        user.append("Existing summary:\n").append(currentSummary == null || currentSummary.isBlank() ? "(none)" : currentSummary.strip());
        user.append("\n\nNew entries to merge:\n");
        for (MemoryEntry entry : batch) {
            user.append('[').append(entry.source().name()).append("][").append(id(entry)).append("] ")
                    .append(entry.content()).append('\n');
        }
        List<LlmMessage> messages = List.of(
                LlmMessage.of(LlmMessageRole.SYSTEM, instruction),
                LlmMessage.of(LlmMessageRole.USER, user.toString()));
        return new LlmRequest(UUID.randomUUID().toString(), messages, List.of(), PromptCacheProfile.COMPRESSION);
    }

    private static String id(MemoryEntry entry) {
        return entry.topic().name().toLowerCase(Locale.ROOT);
    }
}
