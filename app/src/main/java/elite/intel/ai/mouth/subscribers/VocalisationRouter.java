package elite.intel.ai.mouth.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.subscribers.events.*;
import elite.intel.companion.CompanionConfig;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;

import java.util.concurrent.CompletableFuture;

public class VocalisationRouter {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();

    /// --- always pass through
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

    @Subscribe
    public void onNavigationVocalisationRequest(NavigationVocalisationEvent event) {
        if (companionVoicesNarration()) return;
        GameEventBus.publish(new VocalisationRequestEvent(event.getText(), NavigationVocalisationEvent.class, false));
    }


    /// --- on/off based on user settings
    @Subscribe
    public void onRadarContactEvent(RadarContactAnnouncementEvent event) {
        if (companionVoicesNarration()) return;
        if (playerSession.isRadarContactAnnouncementOn()) {
            GameEventBus.publish(new VocalisationRequestEvent(event.getText(), RadarContactAnnouncementEvent.class, false));
        }
    }

    @Subscribe
    public void onDiscoveryAnnouncementEvent(DiscoveryAnnouncementEvent event) {
        if (companionVoicesNarration()) return;
        if (playerSession.isDiscoveryAnnouncementOn()) {
            GameEventBus.publish(new VocalisationRequestEvent(event.getText(), DiscoveryAnnouncementEvent.class, true));
        }
    }

    @Subscribe
    public void onMiningAnnouncementEvent(MiningAnnouncementEvent event) {
        if (companionVoicesNarration()) return;
        if (playerSession.isMiningAnnouncementOn()) {
            GameEventBus.publish(new VocalisationRequestEvent(event.getText(), MiningAnnouncementEvent.class, false));
        }
    }

    @Subscribe
    public void onRouteAnnouncementEvent(RouteAnnouncementEvent event) {
        if (companionVoicesNarration()) return;
        if (playerSession.isRouteAnnouncementOn()) {
            GameEventBus.publish(new VocalisationRequestEvent(event.getText(), RouteAnnouncementEvent.class, true));
        }
    }

    /**
     * In companion mode the companion voices (and remembers) these curated announcements via
     * {@code CompanionAnnouncementBridge}; the legacy TTS path stays silent for them to avoid double speech.
     * Radio transmission and the always-pass-through events (AI response, mission-critical, voice demo) are
     * not affected.
     */
    private static boolean companionVoicesNarration() {
        return CompanionConfig.companionModeOn();
    }

    @Subscribe
    public void onRadioTransmissionEvent(RadioTransmissionEvent event) {
        if (playerSession.isRadioTransmissionOn()) {
            String shipVoice = shipManager.getShip().getVoice();
            String voice;
            if (systemSession.useLocalTTS()) {
                KokoroVoices[] allVoices = KokoroVoices.values();
                KokoroVoices[] voices = java.util.Arrays.stream(allVoices)
                        .filter(v -> !v.name().equals(shipVoice))
                        .toArray(KokoroVoices[]::new);
                voice = voices.length > 0 ? voices[(int) (Math.random() * voices.length)].name() : allVoices[0].name();
            } else {
                GoogleVoices[] allVoices = GoogleVoices.values();
                GoogleVoices[] voices = java.util.Arrays.stream(allVoices)
                        .filter(v -> !v.name().equals(shipVoice))
                        .toArray(GoogleVoices[]::new);
                voice = voices.length > 0 ? voices[(int) (Math.random() * voices.length)].name() : allVoices[0].name();
            }
            GameEventBus.publish(new VocalisationRequestEvent(event.getText(), voice, RadioTransmissionEvent.class, true, true));
        }
    }
}
