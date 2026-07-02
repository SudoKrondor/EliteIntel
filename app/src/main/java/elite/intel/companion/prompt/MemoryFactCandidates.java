package elite.intel.companion.prompt;

import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;

import java.util.ArrayList;
import java.util.List;

/**
 * Single owner of the pre-turn memory answer-candidate selection: given the commander's current input, it
 * pulls the top ranked memory matches (the same unified recall path as {@code search_in_memory}) and filters
 * them down to a few clean, durable answer facts to inline in the prompt as "Relevant remembered facts".
 * <p>
 * Only <b>tier-2</b> facts qualify - the durable, safe subset of the raw timeline (tier-1). The raw history
 * (repeated action logs, the companion's own paraphrases/echoes/hallucinations, "I'm not sure" fillers) is
 * never inlined: those would read to the model as reliable context and steer answers wrong. The filter is:
 * <ul>
 *   <li>drop {@link MemorySource#TOOL_RESULT} (action logs) and {@link MemorySource#SYSTEM} (markers/summary);</li>
 *   <li>keep {@link MemorySource#EVENT} facts (curated at capture, and already relevance-gated by recall);</li>
 *   <li>otherwise keep only {@link MemoryImportance#HIGH} or {@link MemoryImportance#MAX} entries - the
 *       durable facts (name, codeword, plan, target, agreement); LOW/NORMAL chatter, acks and echoes drop out.</li>
 * </ul>
 * Recall already applies its own relevance floor, so every returned entry is a real match; this only removes
 * noise and caps the count. Empty result -&gt; no block is added to the prompt.
 */
public final class MemoryFactCandidates {

    /** How many ranked matches to pull before tier-2 filtering (a generous pool so noise dropping out never starves the real facts). */
    private static final int CANDIDATE_POOL = 20;
    /** Max clean facts inlined into the prompt (enough to answer a two-fact/coherence question, not enough to clutter). */
    private static final int MAX_CANDIDATES = 3;

    private MemoryFactCandidates() {
    }

    /**
     * The clean answer facts to inline for the current commander input, most relevant first, at most
     * {@value #MAX_CANDIDATES}. The entry content is returned verbatim (already lower-cased in the store).
     */
    public static List<String> forInput(MemoryGateway memory, String input) {
        if (memory == null || input == null || input.isBlank()) {
            return List.of();
        }
        List<String> facts = new ArrayList<>();
        for (MemoryEntry entry : memory.recallCandidates(input, CANDIDATE_POOL)) {
            if (isTier2(entry)) {
                facts.add(entry.content());
                if (facts.size() >= MAX_CANDIDATES) {
                    break;
                }
            }
        }
        return List.copyOf(facts);
    }

    /** Whether an entry is a durable, safe answer fact (see class doc). */
    private static boolean isTier2(MemoryEntry entry) {
        return switch (entry.source()) {
            // Action logs, dangerous-action markers and the rolled-up summary are never answer facts.
            case TOOL_RESULT, SYSTEM -> false;
            // Curated at capture and already relevance-gated by recall.
            case EVENT -> true;
            // The commander's own words: a stated fact even at NORMAL (docking-code callsign, field name, plan);
            // only LOW idle banter drops out. Commander questions are never filed, so these are statements.
            case COMMANDER -> entry.importance().compareTo(MemoryImportance.NORMAL) >= 0;
            // The companion's own lines: keep only HIGH/MAX facts; NORMAL/LOW are acks, echoes and hedges - the
            // self-poisoning noise this filter exists to exclude.
            case COMPANION -> entry.importance().compareTo(MemoryImportance.HIGH) >= 0;
        };
    }
}
