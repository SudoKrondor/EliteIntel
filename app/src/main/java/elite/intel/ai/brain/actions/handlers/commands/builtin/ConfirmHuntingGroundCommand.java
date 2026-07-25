package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Self-describing "confirm hunting ground" command.
 * Owns its own execution: body migrated 1:1 from the legacy ConfirmHuntingGroundHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ConfirmHuntingGroundCommand implements IntelCommand {
    public static final String ID = "confirm_hunting_ground";

    @Override
    public String llmDescription() {
        return "Confirm the current star system as a verified pirate-massacre hunting ground (resource extraction site).";
    }


    private final HuntingGroundManager missionDataManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /**
     * Recon verdict: confirms the current system has a Resource Extraction Site once the commander has
     * flown there and spotted it. Detecting a RES and evaluating a hunting ground is only possible while
     * piloting the main ship in that system, so it is offered only there.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        missionDataManager.confirmTargetReconResourceSite(location.getStarName());
        return StringUtls.localizedResponse("handler.pirate.huntingGroundConfirmed");
    }
}
