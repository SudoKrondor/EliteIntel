package elite.intel.ai.mouth.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.subscribers.events.*;
import elite.intel.eventbus.GameEventBus;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;

public class VocalisationRouter {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();

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

    @Subscribe
    public void onVoiceDemoEvent(AiVoxDemoEvent event) {
        publishToMouth(new VocalisationRequestEvent(
                event.getText(), event.getVoiceName(), AiVoxDemoEvent.class, true));
    }

    // The companion no longer routes any speech through here: spontaneous callouts and command/query/macro
    // outcomes are voiced by the companion directly (CompanionNarrator / recordOutcome / the speech gateway).
    // What remains is genuinely system speech (AI response, mission-critical, voice demo, radio), voiced by the
    // legacy TTS in every mode.

    @Subscribe
    public void onRadioTransmissionEvent(RadioTransmissionEvent event) {
        boolean isCyrillic = systemSession.getLanguage() == Language.RU || systemSession.getLanguage() == Language.UK;
        if (playerSession.isRadioTransmissionOn() && !isCyrillic) {
            // Radio is always voiced by Kokoro (even when Google is the main mouth), on a distinct
            // random voice from the commander's own voice so the two speakers sound different.
            String ownVoice = systemSession.getKokoroVoice().name();
            KokoroVoices[] allVoices = KokoroVoices.values();
            KokoroVoices[] voices = java.util.Arrays.stream(allVoices)
                    .filter(v -> !v.name().equals(ownVoice))
                    .toArray(KokoroVoices[]::new);
            String voice = voices.length > 0
                    ? voices[(int) (Math.random() * voices.length)].name()
                    : allVoices[0].name();
            publishToMouth(new VocalisationRequestEvent(
                    event.getText(), voice, RadioTransmissionEvent.class, true, true));
        }
    }

    private static void publishToMouth(VocalisationRequestEvent request) {
        GameEventBus.publish(request);
        // Reentrant Guava posts are queued: from an AiVoxResponseEvent subscriber, publish() returns before the
        // Mouth sees this request. Check ownership only after that queue has drained.
        GameEventBus.afterCurrentDispatch(() -> request.handle().rejectIfUnclaimed(new IllegalStateException(
                "No active Mouth accepted vocalisation request " + request.handle().requestId())));
    }
}
