package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Self-describing "navigate to landing zone" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToLandingZone,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToLandingZoneCommand implements IntelCommand {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return CommandIds.NAVIGATE_TO_LANDING_ZONE;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {

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
