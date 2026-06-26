package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.speech.SpeechRequest;

import java.util.concurrent.CompletableFuture;

/**
 * A thought born from a curated announcement that already carries finished text (mining/discovery/route/
 * radar/navigation callouts the subscriber layer prepared, or a command/macro's own narration bridged from
 * its {@code AiVoxResponseEvent}/{@code MissionCriticalAnnouncementEvent}). Unlike {@link NarrationThought} it
 * does not engage the LLM at all: the line is already worded, so re-phrasing it would only risk mangling
 * curated output and cost a round. Its tiny turn remembers the line as the companion's own words and then
 * voices it verbatim in the companion's voice. Its memory tag is the topic the bridge assigned; like any
 * non-commander thought it never moves the global conversation topic.
 * <p>
 * When a synchronous caller bridged the line (e.g. a macro SPEAK step that blocks until playback ends), an
 * optional {@code spokenSignal} is completed when the companion's playback finishes, so the caller waits the
 * same as on the legacy path - not merely until this thought returns (TTS is async).
 */
public final class VerbatimNarrationThought extends Thought {

    private final ConversationTopic eventTopic;
    private final CompletableFuture<Void> spokenSignal;

    VerbatimNarrationThought(Urgency urgency, String text, ConversationTopic eventTopic, ThoughtContext ctx) {
        this(urgency, text, eventTopic, ctx, null);
    }

    VerbatimNarrationThought(Urgency urgency, String text, ConversationTopic eventTopic, ThoughtContext ctx,
                             CompletableFuture<Void> spokenSignal) {
        super(ThoughtSource.NARRATION, urgency, text, ctx);
        this.eventTopic = eventTopic;
        this.spokenSignal = spokenSignal;
    }

    /** Remember the line first, then voice it verbatim - no LLM, no tools. */
    @Override
    public void run() {
        recordCompanionSpeech(currentInput);
        CompletableFuture<Void> played = ctx.speechGateway().submit(new SpeechRequest(newId(), currentInput, urgency()));
        if (spokenSignal != null) {
            // Mirror the legacy AiVoxResponseEvent future: complete the caller's signal when playback ends.
            played.whenComplete((v, ex) -> {
                if (ex != null) {
                    spokenSignal.completeExceptionally(ex);
                } else {
                    spokenSignal.complete(null);
                }
            });
        }
    }

    @Override
    protected ConversationTopic memoryTopic() {
        return eventTopic;
    }
}
