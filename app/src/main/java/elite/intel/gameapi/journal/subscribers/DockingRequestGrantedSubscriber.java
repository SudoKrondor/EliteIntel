package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.RadioTransmissionEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.carrier.OurCarriers;
import elite.intel.gameapi.journal.events.DockingGrantedEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.PlayerSession;

import java.util.Optional;

import static elite.intel.util.StringUtls.localizedEvent;

public class DockingRequestGrantedSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onDockingRequestGrantedEvent(DockingGrantedEvent event) {
        String playerName = playerSession.getVariablePlayerName();

        // Either of ours: the squadron carrier welcomes the commander home just as the fleet carrier does,
        // and both can have been given a voice for their traffic control.
        Optional<OurCarriers.Ours> ours = OurCarriers.byCallSign(event.getStationName());
        if (ours.isPresent()) {
            CarrierDataDto carrier = ours.get().data();
            String carrierName = carrier.getCarrierName();
            GameEventBus.publish(new RadioTransmissionEvent(
                    localizedEvent("event.docking.trafficControl",
                            carrierName,
                            event.getLandingPad(),
                            localizedEvent("event.docking.welcomeHome", playerName)),
                    localizedEvent("event.trafficControl.speaker", carrierName),
                    carrier.getVoice(),
                    OurCarriers.assignedVoices()
            ));
        } else {
            GameEventBus.publish(new RadioTransmissionEvent(
                    localizedEvent("event.docking.trafficControl",
                            event.getStationName(),
                            event.getLandingPad(),
                            localizedEvent("event.docking.goodToSeeYou", playerName)),
                    localizedEvent("event.trafficControl.speaker", event.getStationName()),
                    null,
                    // Not our carrier: it may not answer in our carrier's voice either.
                    OurCarriers.assignedVoices()
            ));
        }
    }
}
