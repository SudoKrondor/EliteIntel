package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.inputs.RoutePlotter;
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
    public static final String ID = "navigate_to_pirate_mission_provider";

    @Override public String llmDescription() { return "Plot a route to the pirate mission provider."; }


    private final HuntingGroundManager huntingGroundManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
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

        if (destination == null) {
            // No known provider elsewhere: kick off a hunting-ground search and report that.
            GameEventBus.publish(new UserInputEvent(" find hunting grounds"));
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.pirate.noKnowingProviders"));
        }

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(destination);
        String message = location.getStarName().equalsIgnoreCase(targetSystem)
                ? StringUtls.localizedLlm("handler.pirate.checkPorts", targetSystem)
                : StringUtls.localizedLlm("handler.pirate.headTo", destination, targetSystem);
        return CommandOutcome.critical(message);
    }
}
