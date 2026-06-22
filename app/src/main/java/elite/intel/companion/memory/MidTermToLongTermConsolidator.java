package elite.intel.companion.memory;

import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.speech.SpeechGateway;

/**
 * Background component that consolidates mid-term entries (evicted into the consolidation buffer) into
 * an updated {@code long_term_summary} via an LLM compression call (see COMPANION_ARCHITECTURE.md §3.7).
 * <p>
 * On consolidation failure the buffer is cleared (raw entries are lost), diagnostics are written, and
 * the commander is always notified via a {@link SpeechGateway} system notification; the failure is
 * not written into companion memory.
 */
public final class MidTermToLongTermConsolidator {

    private final MemoryGateway memoryGateway;
    private final LlmGateway llmGateway;
    private final SpeechGateway speechGateway;

    public MidTermToLongTermConsolidator(MemoryGateway memoryGateway, LlmGateway llmGateway, SpeechGateway speechGateway) {
        this.memoryGateway = memoryGateway;
        this.llmGateway = llmGateway;
        this.speechGateway = speechGateway;
    }

    /** Buffers an entry evicted from mid-term memory; consolidates when the budget threshold is hit. */
    public void onEvicted(MemoryEntry entry) {
        // TODO: Phase 4 - accumulate into consolidation buffer; consolidate on threshold.
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    /** Runs one compression pass: current summary + buffer -> new summary, then clears the buffer. */
    private void consolidate() {
        // TODO: Phase 4 - LlmGateway compression call and atomic summary replace.
        throw new UnsupportedOperationException("TODO: Phase 4");
    }
}
