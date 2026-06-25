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

            if (isPirateMessage(event.getMessageLocalised()) && haveCargo && !isRadioOn) {
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

    private boolean isPirateMessage(String message) {
        if (message == null) return false;
        Set<String> pirateTransmissions = new HashSet<>();
        pirateTransmissions.add("Big haul like that, surprised you made it this far".toLowerCase());
        pirateTransmissions.add("Carrying anything nice?".toLowerCase());
        pirateTransmissions.add("Do you have anything of value?".toLowerCase());
        pirateTransmissions.add("I hope you have something good in your hold.".toLowerCase());
        pirateTransmissions.add("I'll pick your bones clean, greenhorn.".toLowerCase());
        pirateTransmissions.add("I see all!".toLowerCase());
        pirateTransmissions.add("I've found my next target and it's you, Commander.".toLowerCase());
        pirateTransmissions.add("Let's see what you are carrying.".toLowerCase());
        pirateTransmissions.add("Let me see what you have.".toLowerCase());
        pirateTransmissions.add("The scan will soon be over.".toLowerCase());
        pirateTransmissions.add("What are you hauling?".toLowerCase());
        pirateTransmissions.add("What do you carry, I wonder?".toLowerCase());
        pirateTransmissions.add("What treats do you carry?".toLowerCase());
        pirateTransmissions.add("What do you have in your cargo hold?".toLowerCase());
        pirateTransmissions.add("Next time you should fill your hold with gold.".toLowerCase());
        pirateTransmissions.add("I'm gonna boil you up!".toLowerCase());

        return pirateTransmissions.contains(message.toLowerCase());
    }
}
