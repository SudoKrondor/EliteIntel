package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.DiscoveryAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.MiningAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.NavigationVocalisationEvent;
import elite.intel.ai.mouth.subscribers.events.RadarContactAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.RouteAnnouncementEvent;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.mind.VerbatimNarrationSink;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Urgency;
import elite.intel.session.PlayerSession;

/**
 * Companion-mode bridge for the curated announcement events (mining, discovery, route, radar, navigation).
 * In companion mode these are voiced by the companion and remembered, instead of the legacy
 * {@code VocalisationRouter -> TTS} path, which stays silent for them while companion mode is on (so they
 * are not spoken twice). The per-feature user toggles remain authoritative here, read from
 * {@link PlayerSession}: an announcement the commander turned off is never narrated. Each event carries
 * finished text, so it is voiced verbatim (no LLM) under a fixed topic.
 * <p>
 * {@code RadioTransmissionEvent} is intentionally not bridged: it stays on the legacy random-voice path and
 * is not recorded in memory.
 * <p>
 * It also bridges the narration a command/query/macro emits inside its own handler ({@code AiVoxResponseEvent}
 * and {@code MissionCriticalAnnouncementEvent}): in companion mode the companion owns that speech too,
 * tagged with the current global topic. A synchronous emitter (a macro SPEAK step that carries a completion
 * future and blocks until playback ends) is honored by completing that future when the companion finishes
 * speaking. The legacy {@code VocalisationRouter} stays silent for both while companion mode is on.
 */
public final class CompanionAnnouncementBridge {

    private final VerbatimNarrationSink dispatcher;
    private final PlayerSession playerSession = PlayerSession.getInstance();

    public CompanionAnnouncementBridge(VerbatimNarrationSink dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Subscribe
    public void onMining(MiningAnnouncementEvent event) {
        if (playerSession.isMiningAnnouncementOn()) {
            dispatcher.submitVerbatimNarration(event.getText(), ConversationTopic.MINING);
        }
    }

    @Subscribe
    public void onDiscovery(DiscoveryAnnouncementEvent event) {
        if (playerSession.isDiscoveryAnnouncementOn()) {
            dispatcher.submitVerbatimNarration(event.getText(), ConversationTopic.EXPLORATION);
        }
    }

    @Subscribe
    public void onRoute(RouteAnnouncementEvent event) {
        if (playerSession.isRouteAnnouncementOn()) {
            dispatcher.submitVerbatimNarration(event.getText(), ConversationTopic.NAVIGATION);
        }
    }

    @Subscribe
    public void onRadarContact(RadarContactAnnouncementEvent event) {
        if (playerSession.isRadarContactAnnouncementOn()) {
            dispatcher.submitVerbatimNarration(event.getText(), ConversationTopic.COMBAT);
        }
    }

    /** Navigation vocalisation has no user toggle (always-pass-through in the legacy router). */
    @Subscribe
    public void onNavigation(NavigationVocalisationEvent event) {
        dispatcher.submitVerbatimNarration(event.getText(), ConversationTopic.NAVIGATION);
    }

    /**
     * A command/query/macro's own narration. Voiced and remembered by the companion under the current global
     * topic. A synchronous emitter passes a completion future (it blocks until playback ends); it is completed
     * when the companion finishes speaking, so the caller waits exactly as on the legacy path.
     */
    @Subscribe
    public void onAiVoxResponse(AiVoxResponseEvent event) {
        dispatcher.submitVerbatimNarration(event.getText(), CompanionRuntime.state().globalTopic(),
                Urgency.NORMAL, event.getCompletionFuture());
    }

    /** A handler's mission-critical line: same bridge, but urgent so it preempts whatever is playing. */
    @Subscribe
    public void onMissionCritical(MissionCriticalAnnouncementEvent event) {
        dispatcher.submitVerbatimNarration(event.getText(), CompanionRuntime.state().globalTopic(),
                Urgency.URGENT, null);
    }
}
