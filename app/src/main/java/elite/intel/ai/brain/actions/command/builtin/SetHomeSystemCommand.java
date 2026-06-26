package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SetCurrentStarAsHomeSystem,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SetHomeSystemCommand implements IntelCommand {
    public static final String ID = "set_home_system";

    @Override public String llmDescription() { return "Set the commander's home system."; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        LocationDao.Coordinates coordinates = LocationManager.getInstance().getGalacticCoordinates();
        if (coordinates == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.homeSystem.noCoords")));
            return;
        }
        LocationDto newHome = locationManager.findPrimaryStar(coordinates.primaryStar());
        if (newHome == null || newHome.getSystemAddress() < 1) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.homeSystem.primaryStarNotFound", coordinates.primaryStar())));
            return;
        }
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.homeSystem.setting", coordinates.primaryStar())));
        playerSession.setHomeSystem(newHome);
    }
}
