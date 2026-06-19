package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
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

/**
 * Self-describing "navigate to pirate mission provider" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToToKnownPirateMassacreMissionProvider,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToPirateMissionProviderCommand implements IntelCommand {

    private final HuntingGroundManager huntingGroundManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return CommandIds.NAVIGATE_TO_PIRATE_MISSION_PROVIDER;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
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
