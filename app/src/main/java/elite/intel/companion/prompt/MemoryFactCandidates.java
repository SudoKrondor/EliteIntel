package elite.intel.companion.prompt;

import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemorySearchMatch;

import java.util.ArrayList;
import java.util.List;

/** Selects a small set of trusted EVENT/SAVED_TEXT memory matches for the prompt's facts block. */
public final class MemoryFactCandidates {

    private static final int CANDIDATE_POOL = 20;
    private static final int MAX_CANDIDATES = 3;

    private MemoryFactCandidates() {
    }

    /** Returns at most three trusted facts for the current commander input. */
    public static List<Fact> forInput(MemoryGateway memory, String input) {
        return forInput(memory, input, null);
    }

    /** Same lookup while optionally reusing a semantic query prepared during intake. */
    public static List<Fact> forInput(MemoryGateway memory, String input, SemanticQuery semanticQuery) {
        if (memory == null || input == null || input.isBlank()) {
            return List.of();
        }
        List<Fact> facts = new ArrayList<>();
        for (MemorySearchMatch match : memory.recallFactCandidates(input, CANDIDATE_POOL, semanticQuery)) {
            facts.add(new Fact(match.entry().content(), sourceLabel(match.kind())));
            if (facts.size() == MAX_CANDIDATES) {
                break;
            }
        }
        return List.copyOf(facts);
    }

    private static String sourceLabel(MemoryKind kind) {
        return switch (kind) {
            case EVENT -> "event";
            case SAVED_TEXT -> "saved_text";
            case DIALOGUE, QUERY -> throw new IllegalArgumentException("Untrusted fact kind: " + kind);
        };
    }
}
