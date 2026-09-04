package elite.intel.ai.mouth.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.*;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.PlayerSession;

public class VocalisationRouter {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    /// --- system speech (errors, warnings, greetings, EventNarrator safety callouts): voiced in every mode.
    @Subscribe
    public void onAiVoxResponseEvent(AiVoxResponseEvent event) {
        boolean canBeInterrupted = event.getCompletionFuture() == null;
        publishToMouth(new VocalisationRequestEvent(
                event.getText(), AiVoxResponseEvent.class, canBeInterrupted, event.getCompletionFuture()));
    }

    @Subscribe
    public void onMissionCriticalAnnouncementEvent(MissionCriticalAnnouncementEvent event) {
        publishToMouth(new VocalisationRequestEvent(event.getText(), MissionCriticalAnnouncementEvent.class, false));
    }

    /**
     * A voice audition, spoken by the engine that will actually use it: the main mouth for a ship voice, and
     * the radio engine with its transmission filter for a carrier's traffic control. A radio audition is not
     * gated on the radio toggle - the commander asked to hear this one by picking it.
     */
    @Subscribe
    public void onVoiceDemoEvent(AiVoxDemoEvent event) {
        publishToMouth(new VocalisationRequestEvent(
                event.getText(), event.getVoiceName(), AiVoxDemoEvent.class, true, event.isRadio(), null));
    }

    // The companion no longer routes any speech through here: spontaneous callouts and command/query/macro
    // outcomes are voiced by the companion directly (CompanionNarrator / recordOutcome / the speech gateway).
    // What remains is genuinely system speech (AI response, mission-critical, voice demo, radio), voiced by the
    // legacy TTS in every mode.

    /**
     * Radio is never the main mouth's job: it is voiced by whichever engine {@code RadioVoicing} names for the
     * commander's language - Kokoro almost everywhere, Edge for the Cyrillic locales it cannot pronounce - on
     * a random voice so the speaker on the other end sounds like a stranger. The voice is drawn by that engine
     * (only it knows its own roster) unless the transmission names one, which happens for the one speaker the
     * commander is not meeting for the first time: their own carrier's traffic control.
     */
    @Subscribe
    public void onRadioTransmissionEvent(RadioTransmissionEvent event) {
        if (!Boolean.TRUE.equals(playerSession.isRadioTransmissionOn())) return;
        publishToMouth(new VocalisationRequestEvent(
                event.getText(), event.getVoiceName(), RadioTransmissionEvent.class, true, true,
                event.getSource(), event.getReservedVoices()));
    }

    private static void publishToMouth(VocalisationRequestEvent request) {
        GameEventBus.publish(request);
        // Reentrant Guava posts are queued: from an AiVoxResponseEvent subscriber, publish() returns before the
        // Mouth sees this request. Check ownership only after that queue has drained.
        GameEventBus.afterCurrentDispatch(() -> request.handle().rejectIfUnclaimed(new IllegalStateException(
                "No active Mouth accepted vocalisation request " + request.handle().requestId())));
    }
}
