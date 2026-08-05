package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.RadioTransmissionEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.DockingGrantedEvent;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

public class DockingRequestGrantedSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onDockingRequestGrantedEvent(DockingGrantedEvent event) {
        String playerName = playerSession.getVariablePlayerName();

        if (event.getStationName().equalsIgnoreCase(playerSession.getFleetCarrierData().getCallSign())) {
            String carrierName = playerSession.getFleetCarrierData().getCarrierName();
            GameEventBus.publish(new RadioTransmissionEvent(
                    localizedEvent("event.docking.trafficControl",
                            carrierName,
                            event.getLandingPad(),
                            localizedEvent("event.docking.welcomeHome", playerName)),
                    localizedEvent("event.trafficControl.speaker", carrierName)
            ));
        } else {
            GameEventBus.publish(new RadioTransmissionEvent(
                    localizedEvent("event.docking.trafficControl",
                            event.getStationName(),
                            event.getLandingPad(),
                            localizedEvent("event.docking.goodToSeeYou", playerName)),
                    localizedEvent("event.trafficControl.speaker", event.getStationName())
            ));
        }
    }
}
