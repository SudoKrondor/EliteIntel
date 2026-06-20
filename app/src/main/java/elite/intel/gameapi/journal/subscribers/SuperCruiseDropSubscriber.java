package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.SupercruiseDestinationDropEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

public class SuperCruiseDropSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final CommandHandlerFactory commandHandlerFactory = CommandHandlerFactory.getInstance();

    @Subscribe
    public void onSuperCruiseDrop(SupercruiseDestinationDropEvent event) {
        Thread.ofVirtual().start(() -> {
            if (event.getThreat() > 0) {
                String instructions = """
                            Notify user about supercruise exit and threat level
                            - level 0 threat level is none
                            - level 1 - 2 threat level low
                            - level 3 - 5 threat level medium
                            - level 6 - 8 threat level high
                        """;
                GameEventBus.publish(new SensorDataEvent(" Dropped from supercruise. Threat level: " + event.getThreat() + ". ", instructions));
            }


            LocationDto location = locationManager.findByMarketId(event.getMarketID());
            if (location.getBodyId() < 1 && location.getSystemAddress() < 1) {
                location.setLocationType(LocationDto.LocationType.STATION);
                location.setSystemAddress(playerSession.getLocationData().getSystemAddress());
                locationManager.save(location);
            }

            String carrierName = playerSession.getFleetCarrierData().getCarrierName();
            if (carrierName != null && event.getType().toUpperCase().startsWith(carrierName.toUpperCase())) {
                GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedEvent("event.supercruise.welcomeHomeCarrier", StringUtls.capitalizeWords(carrierName))));
            }
        });
    }
}
