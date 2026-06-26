package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Urgency;

import java.util.concurrent.CompletableFuture;

/**
 * The narration-submission seam the {@code CompanionAnnouncementBridge} depends on: it only needs to hand a
 * finished, already-worded line to the companion to be voiced and remembered verbatim, not the whole
 * {@link ThoughtDispatcher}. Implemented by {@link ThoughtDispatcher}; injected as an interface so the bridge
 * is unit-testable without spinning up the lanes.
 */
public interface VerbatimNarrationSink {

    /** Verbatim narration at the default urgency, with no synchronous-caller signal. */
    void submitVerbatimNarration(String text, ConversationTopic topic);

    /**
     * Verbatim narration with an explicit urgency and an optional {@code spokenSignal} completed when playback
     * ends (used to bridge a command/macro's own narration so a synchronous caller waits for playback).
     */
    void submitVerbatimNarration(String text, ConversationTopic topic, Urgency urgency,
                                 CompletableFuture<Void> spokenSignal);
}
