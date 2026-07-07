package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Self-describing "navigate to landing zone" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToLandingZone,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToLandingZoneCommand implements IntelCommand {
    public static final String ID = "navigate_to_landing_zone";

    @Override public String llmDescription() { return "Plot a route to the landing zone."; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /**
     * Navigates to the landing-zone coordinates the app already remembers for the current location - it infers no
     * parameters from the commander, so it is offered in any control mode (ship, SRV, fighter, on foot). When no
     * landing coordinates are known, {@link #execute} says so rather than the command being hidden.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() || status.isInSrv() || status.isInFighter() || status.isOnFoot();
    }

    @Override
    public void execute(JsonObject params, String responseText) {

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        TargetLocation targetLocation = new TargetLocation();
        if (currentLocation.getLandingCoordinates() == null || currentLocation.getLandingCoordinates().length == 0) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.navigate.landingZoneNotAvailable")));
            return;
        }

        targetLocation.setLatitude(currentLocation.getLandingCoordinates()[0]);
        targetLocation.setLongitude(currentLocation.getLandingCoordinates()[1]);
        targetLocation.setEnabled(true);
        targetLocation.setRequestedTime(System.currentTimeMillis());
        playerSession.setTracking(targetLocation);

        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.navigate.startingNavLandingZone")));
    }
}
