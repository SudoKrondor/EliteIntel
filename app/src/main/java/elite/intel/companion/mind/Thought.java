package elite.intel.companion.mind;

import elite.intel.companion.confirm.DangerousActionConfirmedEvent;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Urgency;

import java.util.ArrayList;
import java.util.List;

/**
 * A unit of work of the consciousness. Owns its topic resolution, local message flow, request
 * handles, dangerous-confirmation waiting, and safe-flush on interrupt.
 * <p>
 * The {@code ThoughtDispatcher} does not know a thought's internal state (awaiting_confirmation,
 * tool-calls, handles, message flow); those belong to the thought.
 */
public final class Thought {

    private final ThoughtSource source;
    private final Urgency urgency;
    private final String currentInput;
    private final ThoughtContext ctx;

    private volatile ConversationTopic topic = ConversationTopic.PENDING;
    private final List<LlmMessage> localMessageFlow = new ArrayList<>();

    /**
     * @param source        which input stream created this thought
     * @param urgency       urgency assigned at birth
     * @param currentInput  commander reply (COMMANDER) or event summary (EVENT); not yet a memory entry
     * @param ctx           shared collaborators
     */
    public Thought(ThoughtSource source, Urgency urgency, String currentInput, ThoughtContext ctx) {
        this.source = source;
        this.urgency = urgency;
        this.currentInput = currentInput;
        this.ctx = ctx;
    }

    /** Runs the thinking loop: compose -> LLM -> resolve topic -> write input -> execute -> ... */
    public void run() {
        // TODO: Phase 2 - drive the consciousness loop per §2.5/§2.6/§2.8/§5.1.
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    /** Interrupts the thought: safe-flush, cancel handles, no new work, then die (§2.7). */
    public void interrupt() {
        // TODO: Phase 3 - interrupt/safe-flush.
        throw new UnsupportedOperationException("TODO: Phase 3");
    }

    /** Delivers a confirmation; effective only while awaiting_confirmation (§2.13). */
    public void onConfirm(DangerousActionConfirmedEvent event) {
        // TODO: Phase 3 - dangerous confirmation.
        throw new UnsupportedOperationException("TODO: Phase 3");
    }

    public ThoughtSource source() {
        return source;
    }

    public Urgency urgency() {
        return urgency;
    }

    public ConversationTopic topic() {
        return topic;
    }
}
