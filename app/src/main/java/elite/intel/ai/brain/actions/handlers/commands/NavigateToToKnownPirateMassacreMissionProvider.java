package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import java.util.List;

public class NavigateToToKnownPirateMassacreMissionProvider implements CommandHandler {

    private final HuntingGroundManager huntingGroundManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();


    @Override public void handle(String action, JsonObject params, String responseText) {
        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        List<MissionProvider> missionProviders = huntingGroundManager.findConfirmedMissionProviders();
        String destination = null;
        String targetSystem = null;
        for (MissionProvider provider : missionProviders) {
            if (!location.getStarName().equalsIgnoreCase(provider.getStarSystem())){
                destination = provider.getStarSystem();
                targetSystem = provider.getTargetSystem();
                break;
            }
        }

        if (location.getStarName().equalsIgnoreCase(targetSystem)){
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.checkPorts", targetSystem)));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.headTo", destination, targetSystem)));
        }

        if (destination == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.noKnowingProviders")));
            EventBusManager.publish(new UserInputEvent(" find hunting grounds"));
        } else {
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(destination);
        }
    }
}
