package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

public class SetCurrentStarAsHomeSystem implements CommandHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public void handle(String action, JsonObject params, String responseText) {
        LocationDao.Coordinates coordinates = LocationManager.getInstance().getGalacticCoordinates();
        if (coordinates == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.homeSystem.noCoords")));
            return;
        }
        LocationDto newHome = locationManager.findPrimaryStar(coordinates.primaryStar());
        if (newHome == null || newHome.getSystemAddress() < 1) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.homeSystem.primaryStarNotFound", coordinates.primaryStar())));
            return;
        }
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.homeSystem.setting", coordinates.primaryStar())));
        playerSession.setHomeSystem(newHome);
    }
}
