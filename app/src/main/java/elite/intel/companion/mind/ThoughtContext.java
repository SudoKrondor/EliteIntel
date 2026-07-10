package elite.intel.companion.mind;

import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;

/**
 * Immutable input-side signals for one thought. Unlike {@link ThoughtDependencies}, which owns shared services,
 * this context travels with a thought from intake through prompt construction and is discarded when that thought ends.
 */
final class ThoughtContext {

    private final ThoughtSource source;
    private final Urgency urgency;
    private final String currentInput;
    private final String matchInput;
    private final SemanticQuery semanticQuery;

    private ThoughtContext(ThoughtSource source, Urgency urgency, String currentInput, String matchInput,
                           SemanticQuery semanticQuery) {
        this.source = source;
        this.urgency = urgency;
        this.currentInput = currentInput;
        this.matchInput = matchInput;
        this.semanticQuery = semanticQuery;
    }

    /** Builds the turn signals for a commander input, whose canonical match text may differ from its raw wording. */
    static ThoughtContext commander(Urgency urgency, String currentInput, String matchInput) {
        return new ThoughtContext(ThoughtSource.COMMANDER, urgency, currentInput, matchInput, null);
    }

    /** Builds the turn signals for an event, whose match text is the LLM-visible event prompt. */
    static ThoughtContext event(Urgency urgency, String currentInput, String matchInput) {
        return new ThoughtContext(ThoughtSource.EVENT, urgency, currentInput, matchInput, null);
    }

    /** Returns a copy carrying the semantic query computed at intake for this exact turn. */
    ThoughtContext withSemanticQuery(SemanticQuery semanticQuery) {
        return this.semanticQuery == semanticQuery ? this
                : new ThoughtContext(source, urgency, currentInput, matchInput, semanticQuery);
    }

    /** The source lane and memory role for this thought. */
    ThoughtSource source() {
        return source;
    }

    /** The urgency chosen when this thought was accepted by the dispatcher. */
    Urgency urgency() {
        return urgency;
    }

    /** The raw user/event text retained for execution and memory. */
    String currentInput() {
        return currentInput;
    }

    /** The canonical input used for tool selection and prompt construction. */
    String matchInput() {
        return matchInput;
    }

    /** The optional semantic query prepared before this thought entered its lane. */
    SemanticQuery semanticQuery() {
        return semanticQuery;
    }
}
