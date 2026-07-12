package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;

/**
 * Consciousness-level runtime state shared across a companion session: the global topic (the conversation's
 * current topic, used to tag the commander's memory entries). It also retains the latest normalized commander
 * input strictly as an observer snapshot for UI refresh; turn routing never reads that shared value.
 * <p>
 * The global topic is changed only by the COMMANDER {@code classify_turn} tool; EVENT thoughts never
 * change it (an event's topic for memory tagging comes from a static event-type map).
 * <p>
 * Ownership: this object will be held by the {@code ThoughtDispatcher} once it exists; until then it is
 * reached statically via {@code CompanionRuntime}. It is a plain mutable holder.
 */
public final class CompanionState {

    private volatile ConversationTopic globalTopic = ConversationTopic.SOCIAL;
    private volatile String lastCommanderMatchInput = "";

    /** The conversation's current global topic; defaults to small talk at session start. */
    public ConversationTopic globalTopic() {
        return globalTopic;
    }

    public void setGlobalTopic(ConversationTopic topic) {
        this.globalTopic = topic;
    }

    /** Latest normalized input for observer/UI refresh only; cognitive work uses its immutable ThoughtContext. */
    public String lastCommanderMatchInput() {
        return lastCommanderMatchInput;
    }

    public void setLastCommanderMatchInput(String input) {
        this.lastCommanderMatchInput = input == null ? "" : input;
    }
}
