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
    /** Monotonic intake timestamp retained solely to attribute one thought's latency diagnostics. */
    private final long acceptedAtNanos;

    private ThoughtContext(ThoughtSource source, Urgency urgency, String currentInput, String matchInput,
                           SemanticQuery semanticQuery, long acceptedAtNanos) {
        this.source = source;
        this.urgency = urgency;
        this.currentInput = currentInput;
        this.matchInput = matchInput;
        this.semanticQuery = semanticQuery;
        this.acceptedAtNanos = acceptedAtNanos;
    }

    /** Builds the turn signals for a commander input, whose canonical match text may differ from its raw wording. */
    static ThoughtContext commander(Urgency urgency, String currentInput, String matchInput) {
        return commander(urgency, currentInput, matchInput, System.nanoTime());
    }

    /** Builds commander turn signals with the intake timestamp captured by the dispatcher. */
    static ThoughtContext commander(Urgency urgency, String currentInput, String matchInput, long acceptedAtNanos) {
        return new ThoughtContext(ThoughtSource.COMMANDER, urgency, currentInput, matchInput, null, acceptedAtNanos);
    }

    /** Builds the turn signals for an event, whose match text is the LLM-visible event prompt. */
    static ThoughtContext event(Urgency urgency, String currentInput, String matchInput) {
        return new ThoughtContext(ThoughtSource.EVENT, urgency, currentInput, matchInput, null, System.nanoTime());
    }

    /** Returns a copy carrying the semantic query computed at intake for this exact turn. */
    ThoughtContext withSemanticQuery(SemanticQuery semanticQuery) {
        return this.semanticQuery == semanticQuery ? this
                : new ThoughtContext(source, urgency, currentInput, matchInput, semanticQuery, acceptedAtNanos);
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

    /** Monotonic timestamp at which this thought's input was accepted, used only for latency diagnostics. */
    long acceptedAtNanos() {
        return acceptedAtNanos;
    }
}
