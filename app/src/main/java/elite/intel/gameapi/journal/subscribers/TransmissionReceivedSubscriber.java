package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.ai.mouth.subscribers.events.RadioTransmissionEvent;
import elite.intel.db.managers.CargoHoldManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.ReceiveTextEvent;
import elite.intel.session.PlayerSession;

import java.util.*;

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

                if (isStation) {
                    if (!event.getMessageLocalised().toLowerCase().contains("fire zone")) {
                        GameEventBus.publish(new RadioTransmissionEvent(localizedEvent("event.transmission.trafficControl", event.getFrom(), event.getMessageLocalised())));
                    }
                } else {
                    GameEventBus.publish(new RadioTransmissionEvent(event.getMessageLocalised()));
                }
            }
        });
    }
}
