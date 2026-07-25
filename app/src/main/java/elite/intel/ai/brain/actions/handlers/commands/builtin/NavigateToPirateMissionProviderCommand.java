package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
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

    @Override
    public String llmDescription() {
        return "Plot a route to a confirmed pirate-massacre mission provider (where the kill missions are collected).";
    }


    private final HuntingGroundManager huntingGroundManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** Route plotting taps the ship-only GalaxyMapOpen bind; works only in the main-ship cockpit. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
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

        // Non-terminal announcement: the route plotting below must still run, so declare the line via
        // CompanionRuntime.narrator().filler (voiced, not remembered) instead of returning here.
        if (location.getStarName().equalsIgnoreCase(targetSystem)){
            CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.pirate.checkPorts", targetSystem), false);
        } else {
            CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.pirate.headTo", destination, targetSystem), false);
        }

        if (destination == null) {
            GameEventBus.publish(new UserInputEvent(" find hunting grounds"));
            return StringUtls.localizedResponse("handler.pirate.noKnowingProviders");
        } else {
            RoutePlotter plotter = new RoutePlotter();
            return plotter.plotRoute(destination);
        }
    }
}
