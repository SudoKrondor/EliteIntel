package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.speech.SpeechRequest;

/**
 * A thought born from a curated announcement that already carries finished text (mining/discovery/route/
 * radar/navigation callouts the subscriber layer prepared). Unlike {@link NarrationThought} it does not
 * engage the LLM at all: the line is already worded, so re-phrasing it would only risk mangling curated
 * output and cost a round. Its tiny turn remembers the line as the companion's own words and then voices it
 * verbatim in the companion's voice. Its memory tag is the topic the bridge assigned; like any non-commander
 * thought it never moves the global conversation topic.
 */
public final class VerbatimNarrationThought extends Thought {

    private final ConversationTopic eventTopic;

    VerbatimNarrationThought(Urgency urgency, String text, ConversationTopic eventTopic, ThoughtContext ctx) {
        super(ThoughtSource.NARRATION, urgency, text, ctx);
        this.eventTopic = eventTopic;
    }

    /** Remember the line first, then voice it verbatim - no LLM, no tools. */
    @Override
    public void run() {
        recordCompanionSpeech(currentInput);
        ctx.speechGateway().submit(new SpeechRequest(newId(), currentInput, urgency()));
    }

    @Override
    protected ConversationTopic memoryTopic() {
        return eventTopic;
    }
}
