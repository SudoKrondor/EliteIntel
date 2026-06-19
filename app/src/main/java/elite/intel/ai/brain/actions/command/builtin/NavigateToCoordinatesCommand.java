package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Self-describing "navigate to coordinates" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToCoordinatesHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToCoordinatesCommand implements IntelCommand {
    public static final String ID = "navigate_to_coordinates";


    private static final Logger log = LogManager.getLogger(NavigateToCoordinatesCommand.class);

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec lat = new ActionParameterSpec(
                "lat",
                "number",
                true,
                "Target latitude on the body surface, in degrees. Valid range -90 to 90.",
                List.of("12.3456", "-45.0"),
                "Planetary latitude the commander wants to navigate to."
        );
        ActionParameterSpec lon = new ActionParameterSpec(
                "lon",
                "number",
                true,
                "Target longitude on the body surface, in degrees. Valid range -180 to 180.",
                List.of("78.9012", "-120.5"),
                "Planetary longitude the commander wants to navigate to."
        );
        lat.validate();
        lon.validate();
        return List.of(lat, lon);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        PlayerSession playerSession = PlayerSession.getInstance();

        if(params.get("lat") == null || params.get("lon") == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.common.sayAgain")));
            return;
        }

        double latitude = params.get("lat").getAsDouble();
        double longitude = params.get("lon").getAsDouble();

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            log.error("Invalid coordinates: " + latitude + ", " + longitude);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.invalidCoords")));
        } else {
            TargetLocation tracking = playerSession.getTracking();
            tracking.setEnabled(true);
            tracking.setLatitude(latitude);
            tracking.setLongitude(longitude);
            tracking.setRequestedTime(System.currentTimeMillis());
            playerSession.setTracking(tracking);
            log.info("Starting navigation to coordinates: " + latitude + ", " + longitude);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.startingNavCoords", latitude, longitude)));
        }
    }
}
