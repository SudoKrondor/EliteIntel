package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;

/**
 * Consciousness-level runtime state shared across a companion session: the global topic (the conversation's
 * current topic, used to tag the commander's memory entries).
 * <p>
 * The global topic is changed only by the COMMANDER {@code classify_turn} tool; EVENT thoughts never
 * change it (an event's topic for memory tagging comes from a static event-type map).
 * <p>
 * Ownership: this object will be held by the {@code ThoughtDispatcher} once it exists; until then it is
 * reached statically via {@code CompanionRuntime}. It is a plain mutable holder.
 */
public final class CompanionState {

    private volatile ConversationTopic globalTopic = ConversationTopic.SOCIAL;

    /** The conversation's current global topic; defaults to small talk at session start. */
    public ConversationTopic globalTopic() {
        return globalTopic;
    }

    public void setGlobalTopic(ConversationTopic topic) {
        this.globalTopic = topic;
    }
}
