package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.memory.MemoryImportance;
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
        try {
            recordCompanionSpeech(currentInput);
            CompletableFuture<Void> played = ctx.speechGateway().submit(new SpeechRequest(newId(), currentInput, urgency()));
            // A synchronous caller waits for playback, not for this thought to return; mirror the legacy
            // AiVoxResponseEvent future by completing its signal when the gateway reports playback finished.
            played.whenComplete((v, ex) -> completeSpokenSignal(ex));
        } catch (RuntimeException startupFailure) {
            // Voicing could not even start: complete the signal now so a synchronous caller (a macro SPEAK
            // step blocked on it) is not stranded for its full timeout. WHY: the caller's own timeout is only
            // the last-resort backstop for the cases this cannot reach - the thought dropped before it runs
            // (forced shutdown) or the gateway never reporting completion.
            completeSpokenSignal(startupFailure);
            throw startupFailure;
        }
    }

    /** Completes the optional synchronous-caller signal once (idempotent); a failure propagates as exceptional. */
    private void completeSpokenSignal(Throwable error) {
        if (spokenSignal == null) {
            return;
        }
        if (error != null) {
            spokenSignal.completeExceptionally(error);
        } else {
            spokenSignal.complete(null);
        }
    }

    @Override
    protected ConversationTopic memoryTopic() {
        return eventTopic;
    }

    /** Verbatim narration carries ordinary importance; only the commander rates a turn (classify_turn). */
    @Override
    protected MemoryImportance memoryImportance() {
        return MemoryImportance.NORMAL;
    }
}
