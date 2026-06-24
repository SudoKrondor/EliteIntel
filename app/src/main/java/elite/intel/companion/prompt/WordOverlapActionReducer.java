package elite.intel.companion.prompt;

import elite.intel.ai.brain.Reducer;
import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.query.ConnectionCheckQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * First {@link CompanionActionReducer} implementation: a thin wrapper over the legacy
 * {@code elite.intel.ai.brain.Reducer} word-overlap selector. It builds the allowed-category candidates
 * (step 1, via {@link GameToolCandidates}), delegates the narrowing to the legacy reducer keyed by each
 * candidate's phrase group, strips the legacy reducer's injected empty-input fallback (the companion has
 * its own speak/nothing_to_do), and returns the surviving tools in candidate order.
 * <p>
 * Notes: a blank input makes the legacy reducer return everything (no narrowing), which is the intended
 * "offer all" behavior. Input normalization is left to the legacy reducer (it lowercases/tokenizes
 * internally); richer normalization is an internal refinement that would not change this contract.
 */
public final class WordOverlapActionReducer implements CompanionActionReducer {

    /** Legacy fallback ids the reducer may inject on no match; never surfaced to the companion. */
    private static final Set<String> FALLBACK_IDS = Set.of(
            GeneralConversationQueryCommand.ID,
            ConnectionCheckQueryCommand.ID,
            IgnoreNonsensicalInputCommand.ID);

    private final Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource;

    /** Production: candidates from the live registries/status/language. */
    public WordOverlapActionReducer() {
        this(new GameToolCandidates()::collect);
    }

    /** Test seam: inject a fixed candidate source. */
    WordOverlapActionReducer(Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource) {
        this.candidateSource = candidateSource;
    }

    @Override
    public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput) {
        List<GameToolCandidates.Candidate> candidates = candidateSource.apply(allowedCategories);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<String, String> phraseToId = new LinkedHashMap<>();
        for (GameToolCandidates.Candidate candidate : candidates) {
            phraseToId.put(candidate.phraseKey(), candidate.id());
        }

        Map<String, String> reduced = Reducer.reduce(currentInput, phraseToId, false);
        Set<String> survivors = new LinkedHashSet<>(reduced.values());
        survivors.removeAll(FALLBACK_IDS);

        // Preserve candidate order; dedup in case an id appears under multiple reducer keys.
        List<LlmToolDefinition> result = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        for (GameToolCandidates.Candidate candidate : candidates) {
            if (survivors.contains(candidate.id()) && added.add(candidate.id())) {
                result.add(candidate.tool());
            }
        }
        return result;
    }
}
