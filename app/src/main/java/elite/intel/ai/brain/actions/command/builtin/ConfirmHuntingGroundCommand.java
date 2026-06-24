package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Self-describing "confirm hunting ground" command.
 * Owns its own execution: body migrated 1:1 from the legacy ConfirmHuntingGroundHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ConfirmHuntingGroundCommand implements IntelCommand {
    public static final String ID = "confirm_hunting_ground";

    @Override public String llmDescription() { return "Confirm the current location as a hunting ground."; }


    private final HuntingGroundManager missionDataManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        missionDataManager.confirmTargetReconResourceSite(location.getStarName());
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.huntingGroundConfirmed")));
    }
}
