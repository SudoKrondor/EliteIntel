package elite.intel.companion.mind;

import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.clarify.PendingClarification;
import elite.intel.companion.model.GameStateSnapshot;
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
    private final GameStateSnapshot gameStateSnapshot;
    private final SemanticQuery semanticQuery;
    private final PendingClarification pendingClarification;
    /** Monotonic intake timestamp retained solely to attribute one thought's latency diagnostics. */
    private final long acceptedAtNanos;

    private ThoughtContext(ThoughtSource source, Urgency urgency, String currentInput, String matchInput,
                           GameStateSnapshot gameStateSnapshot, SemanticQuery semanticQuery,
                           PendingClarification pendingClarification, long acceptedAtNanos) {
        this.source = source;
        this.urgency = urgency;
        this.currentInput = currentInput;
        this.matchInput = matchInput;
        this.gameStateSnapshot = gameStateSnapshot;
        this.semanticQuery = semanticQuery;
        this.pendingClarification = pendingClarification;
        this.acceptedAtNanos = acceptedAtNanos;
    }

    /** Builds the turn signals for a commander input, whose canonical match text may differ from its raw wording. */
    static ThoughtContext commander(Urgency urgency, String currentInput, String matchInput) {
        return commander(urgency, currentInput, matchInput, System.nanoTime(), GameStateSnapshot.capture());
    }

    /** Builds commander turn signals with the intake timestamp captured by the dispatcher. */
    static ThoughtContext commander(Urgency urgency, String currentInput, String matchInput, long acceptedAtNanos) {
        return commander(urgency, currentInput, matchInput, acceptedAtNanos, GameStateSnapshot.capture());
    }

    /** Builds commander turn signals with an already-captured immutable routing state. */
    static ThoughtContext commander(Urgency urgency, String currentInput, String matchInput,
                                    GameStateSnapshot gameStateSnapshot) {
        return commander(urgency, currentInput, matchInput, System.nanoTime(), gameStateSnapshot);
    }

    /** Builds commander turn signals with both intake time and routing state captured by the dispatcher. */
    static ThoughtContext commander(Urgency urgency, String currentInput, String matchInput, long acceptedAtNanos,
                                    GameStateSnapshot gameStateSnapshot) {
        return new ThoughtContext(ThoughtSource.COMMANDER, urgency, currentInput, matchInput,
                gameStateSnapshot, null, null, acceptedAtNanos);
    }

    /** Builds the turn signals for an event, whose match text is the LLM-visible event prompt. */
    static ThoughtContext event(Urgency urgency, String currentInput, String matchInput) {
        return new ThoughtContext(ThoughtSource.EVENT, urgency, currentInput, matchInput,
                null, null, null, System.nanoTime());
    }

    /** Returns a copy carrying the semantic query computed at intake for this exact turn. */
    ThoughtContext withSemanticQuery(SemanticQuery semanticQuery) {
        return this.semanticQuery == semanticQuery ? this
                : new ThoughtContext(source, urgency, currentInput, matchInput,
                gameStateSnapshot, semanticQuery, pendingClarification, acceptedAtNanos);
    }

    /** Returns a commander-context copy that owns one claimed cross-turn clarification. */
    ThoughtContext withPendingClarification(PendingClarification pendingClarification) {
        if (source != ThoughtSource.COMMANDER) {
            throw new IllegalStateException("Only commander thoughts may carry a pending clarification");
        }
        return this.pendingClarification == pendingClarification ? this
                : new ThoughtContext(source, urgency, currentInput, matchInput,
                gameStateSnapshot, semanticQuery, pendingClarification, acceptedAtNanos);
    }

    /** The source lane and memory role for this thought. */
    ThoughtSource source() {
        return source;
    }

    /** The urgency chosen when this thought was accepted by the dispatcher. */
    Urgency urgency() {
        return urgency;
    }

    /** The raw user/event text retained for command execution and intake diagnostics. */
    String currentInput() {
        return currentInput;
    }

    /** The canonical input used for tool selection and prompt construction. */
    String matchInput() {
        return matchInput;
    }

    /**
     * The text to retain if this turn reaches a memory-producing settlement: canonical commander wording, or the
     * original event stimulus. Event match text may be an expanded LLM prompt rather than the stimulus itself, so
     * it must not be stored.
     */
    String memoryInput() {
        return source == ThoughtSource.COMMANDER ? matchInput : currentInput;
    }

    /** The immutable game-state inputs used for every visibility decision in this commander turn. */
    GameStateSnapshot gameStateSnapshot() {
        return gameStateSnapshot;
    }

    /** The optional semantic query prepared before this thought entered its lane. */
    SemanticQuery semanticQuery() {
        return semanticQuery;
    }

    /** The clarification atomically claimed for this commander turn, or {@code null} for a fresh turn. */
    PendingClarification pendingClarification() {
        return pendingClarification;
    }

    /** Monotonic timestamp at which this thought's input was accepted, used only for latency diagnostics. */
    long acceptedAtNanos() {
        return acceptedAtNanos;
    }
}
