package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.DiscoveryAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.MiningAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.NavigationVocalisationEvent;
import elite.intel.ai.mouth.subscribers.events.RadarContactAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.RouteAnnouncementEvent;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.model.ConversationTopic;
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
 */
public final class CompanionAnnouncementBridge {

    private final ThoughtDispatcher dispatcher;
    private final PlayerSession playerSession = PlayerSession.getInstance();

    public CompanionAnnouncementBridge(ThoughtDispatcher dispatcher) {
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
}
