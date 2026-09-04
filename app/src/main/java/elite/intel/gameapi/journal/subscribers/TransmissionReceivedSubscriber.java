package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.ai.mouth.subscribers.events.RadioTransmissionEvent;
import elite.intel.db.managers.CargoHoldManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.StationName;
import elite.intel.gameapi.carrier.OurCarriers;
import elite.intel.gameapi.journal.events.ReceiveTextEvent;
import elite.intel.session.PlayerSession;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class TransmissionReceivedSubscriber {

    private static final int DEDUP_CACHE_SIZE = 50;
    private final Set<String> recentTransmissions = Collections.newSetFromMap(
            new LinkedHashMap<>(DEDUP_CACHE_SIZE + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> e) {
                    return size() > DEDUP_CACHE_SIZE;
                }
            }
    );

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onReceiveTextEvent(ReceiveTextEvent event) {
        Thread.ofVirtual().start(() -> {
            String key = event.getFrom() + "|" + event.getMessageLocalised();
            synchronized (recentTransmissions) {
                if (!recentTransmissions.add(key)) return;
            }

            Boolean isRadioOn = playerSession.isRadioTransmissionOn();
            CargoHoldManager cargoHoldManager = CargoHoldManager.getInstance();
            boolean haveCargo = cargoHoldManager.get() != null && cargoHoldManager.get().getCount() > 0;

            if (event.isPirateMessage() && haveCargo && !isRadioOn) {
                EventNarrator.critical(localizedEvent("event.pirate.alert"));
                return;
            }

            if (isRadioOn == null || !isRadioOn) return;

            if (event.getMessageLocalised() != null && !event.getMessageLocalised().toLowerCase().contains("entered channel")) {
                boolean isStation = event.getMessage().toLowerCase().contains("station");

                if (event.getFrom().toLowerCase().contains("cruise")) return;
                if (event.getFrom().toLowerCase().contains("military")) return;
                if (event.getMessage().contains("$STATION_docking_granted;")) return;

                // The sender as the game names it for a human: From is a symbol like
                // "$ShipName_Police_Federation;" for anything that is not a commander, and a colonisation
                // ship signs its traffic control "$EXT_PANEL_ColonisationShip; Schroter's Progress" with no
                // localised sibling at all - so the fallback goes through StationName rather than straight
                // to the mouth.
                String source = event.getFromLocalised() == null || event.getFromLocalised().isBlank()
                        ? StationName.display(event.getFrom())
                        : event.getFromLocalised();

                // Our own carrier's traffic control speaks with the voice the commander gave it, if any.
                // Matched on the raw From, which is where the callsign is: a carrier signs its transmissions
                // with its name and callsign together ("LONE WOLF GHY-L8X").
                String voice = OurCarriers.radioVoiceOf(event.getFrom());
                // Nobody else draws a voice a carrier answers on, or a passing station would reply in the
                // commander's own carrier's voice.
                Set<String> reserved = OurCarriers.assignedVoices();

                if (isStation) {
                    if (!event.getMessageLocalised().toLowerCase().contains("fire zone")) {
                        // One resolution of the sender for both: the spoken line and the label on screen
                        // name the same station, rather than disagreeing whenever the journal localises it.
                        GameEventBus.publish(new RadioTransmissionEvent(
                                localizedEvent("event.transmission.trafficControl", source, event.getMessageLocalised()),
                                localizedEvent("event.trafficControl.speaker", source),
                                voice, reserved));
                    }
                } else {
                    GameEventBus.publish(new RadioTransmissionEvent(
                            event.getMessageLocalised(), source, voice, reserved));
                }
            }
        });
    }
}
