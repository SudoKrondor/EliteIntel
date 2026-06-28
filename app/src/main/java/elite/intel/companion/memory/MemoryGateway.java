package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.ConversationTopic;

import java.util.List;

/**
 * The single door to the companion's session memory. Owns the memory areas (short-term, mid-term topic,
 * long_term_summary) and the transitions between them; no one accesses the internal memory levels directly.
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
     * Unified recall (the {@code search_in_memory} system function): searches all stored memory - the
     * short-term timeline plus mid-term topic memory across every topic - for entries whose content matches the
     * query, and returns the matches ranked by importance, then recency, at most {@code limit}.
     *
     * @param query plain-text filter; blank returns the most recent entries regardless of content
     * @param limit maximum entries to return
     */
    List<String> recallMatching(String query, int limit);

    /**
     * The always-on working set: the most recent important (HIGH/MAX) mid-term entries, for inlining into the
     * prompt so durable facts that aged out of short-term stay visible without a recall. Short-term is not
     * included - it is already inlined whole and searched by {@link #recallMatching}. Returned oldest-to-newest.
     *
     * @param maxEntries  cap on entries returned
     * @param tokenBudget soft token ceiling for the slice (at least the newest entry is always kept)
     */
    List<MemoryEntry> importantWorkingSet(int maxEntries, int tokenBudget);

    /** Cheap index metadata for the prompt (no content loaded). */
    MemoryAvailabilitySnapshot indexes();

    /** The single session-wide long-term summary, always added to the prompt. */
    String longTermSummary();

    /** Atomically replaces the long-term summary (called by {@code MidTermToLongTermConsolidator}). */
    void replaceLongTermSummary(String summary);

    /**
     * The pinned MAX-importance facts in long-term memory (verbatim, never compressed), always surfaced in the
     * prompt. Oldest-to-newest.
     */
    List<MemoryEntry> longTermPinnedFacts();

    /** Pins a MAX-importance fact verbatim into long-term memory (called by {@code MidTermToLongTermConsolidator}). */
    void addLongTermPinned(MemoryEntry fact);
}
