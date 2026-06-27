package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.ConversationTopic;

import java.util.List;

/**
 * The single door to the companion's session memory. Owns the four memory areas (short-term,
 * mid-term topic, long_term_summary, llm_memory) and the transitions between them; no one accesses
 * the internal memory levels directly.
 * <p>
 * The gateway is mechanical: it stores/retrieves but never interprets meaning, decides importance,
 * calls the LLM, summarizes, or changes topic.
 */
public interface MemoryGateway {

    /** Writes a normal entry. New entries land in short-term memory first. */
    void write(MemoryEntry entry);

    /** Returns the hot short-term timeline, oldest-to-newest, for the prompt context block. */
    List<MemoryEntry> readShortTermTimeline();

    /**
     * Topic-scoped recall over mid-term memory (COMMANDER-only at the call site).
     *
     * @param topic  required topic to read
     * @param query  optional plain-text filter within the topic (null/blank = latest entries)
     * @param limit  maximum entries to return
     */
    List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit);

    /**
     * Unified recall (the {@code recall(query)} system function): searches all stored memory - the short-term
     * timeline plus mid-term topic memory across every topic plus the conscious llm_memory facts - for entries
     * whose content matches the query, and returns the most recent matches (newest first), at most {@code limit}.
     *
     * @param query plain-text filter; blank returns the most recent entries regardless of content
     * @param limit maximum entries to return
     */
    List<String> recallMatching(String query, int limit);

    /** Returns the entire llm_memory list (small enough to return whole). */
    List<String> readLlmMemory();

    /** Writes a short fact to the cyclic llm_memory (code enforces length/dedup limits). */
    void writeLlmMemory(String content);

    /** Cheap index metadata for the prompt (no content loaded). */
    MemoryAvailabilitySnapshot indexes();

    /** The single session-wide long-term summary, always added to the prompt. */
    String longTermSummary();

    /** Atomically replaces the long-term summary (called by {@code MidTermToLongTermConsolidator}). */
    void replaceLongTermSummary(String summary);
}
