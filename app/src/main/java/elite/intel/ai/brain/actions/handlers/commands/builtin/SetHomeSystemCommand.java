package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SetCurrentStarAsHomeSystem,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SetHomeSystemCommand implements IntelCommand {
    public static final String ID = "set_home_system";

    @Override
    public String llmDescription() {
        return "Set the commander's current star system as the home system.";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** App-side bookkeeping (tags current system as home); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        LocationDao.Coordinates coordinates = LocationManager.getInstance().getGalacticCoordinates();
        if (coordinates == null) {
            return StringUtls.localizedResponse("handler.homeSystem.noCoords");
        }
        LocationDto newHome = locationManager.findPrimaryStar(coordinates.primaryStar());
        if (newHome == null || newHome.getSystemAddress() < 1) {
            return StringUtls.localizedResponse("handler.homeSystem.primaryStarNotFound", coordinates.primaryStar());
        }

        playerSession.setHomeSystem(newHome);
        return StringUtls.localizedResponse("handler.homeSystem.setting", coordinates.primaryStar());
    }
}
