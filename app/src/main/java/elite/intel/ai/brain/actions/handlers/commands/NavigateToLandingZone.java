package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

public class NavigateToLandingZone implements CommandHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public void handle(String action, JsonObject params, String responseText) {

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        TargetLocation targetLocation = new TargetLocation();
        if (currentLocation.getLandingCoordinates() == null || currentLocation.getLandingCoordinates().length == 0) {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.navigate.landingZoneNotAvailable")));
            return;
        }

        targetLocation.setLatitude(currentLocation.getLandingCoordinates()[0]);
        targetLocation.setLongitude(currentLocation.getLandingCoordinates()[1]);
        targetLocation.setEnabled(true);
        targetLocation.setRequestedTime(System.currentTimeMillis());
        playerSession.setTracking(targetLocation);

        EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.navigate.startingNavLandingZone")));
    }

}
