package elite.intel.ai.mouth.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.subscribers.events.*;
import elite.intel.eventbus.GameEventBus;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;

import java.util.concurrent.CompletableFuture;

public class VocalisationRouter {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();

    /// --- system speech (errors, warnings, greetings, EventNarrator safety callouts): voiced in every mode.
    @Subscribe
    public void onAiVoxResponseEvent(AiVoxResponseEvent event) {
        boolean canBeInterrupted = event.getCompletionFuture() == null;
        CompletableFuture<Void> completionFuture = event.getCompletionFuture();
        if (canBeInterrupted) {
            // Regular AI response: track playback end so STT is suppressed while the AI is speaking.
            completionFuture = new CompletableFuture<>();
            CompletableFuture<Void> cf = completionFuture;
            GameEventBus.publish(new IsSpeakingEvent(true));
            cf.whenComplete((v, t) -> GameEventBus.publish(new IsSpeakingEvent(false)));
        }
        GameEventBus.publish(new VocalisationRequestEvent(event.getText(), AiVoxResponseEvent.class, canBeInterrupted, completionFuture));
    }

    @Subscribe
    public void onMissionCriticalAnnouncementEvent(MissionCriticalAnnouncementEvent event) {
        GameEventBus.publish(new VocalisationRequestEvent(event.getText(), MissionCriticalAnnouncementEvent.class, false));
    }

    @Subscribe
    public void onVoiceDemoEvent(AiVoxDemoEvent event) {
        GameEventBus.publish(new VocalisationRequestEvent(event.getText(), event.getVoiceName(), AiVoxDemoEvent.class, true));
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
            GameEventBus.publish(new VocalisationRequestEvent(event.getText(), voice, RadioTransmissionEvent.class, true, true));
        }
    }
}
