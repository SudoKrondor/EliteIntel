package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.dao.PirateHuntingGroundsDao.HuntingGround;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.HuntingGroundManager.PirateMissionTuple;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Map;

/**
 * Self-describing "navigate to mission provider" command.
 * Owns its own execution: body migrated 1:1 from the legacy ReconMissionProviderSystemHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToMissionProviderCommand implements IntelCommand {
    public static final String ID = "navigate_to_mission_provider";

    @Override
    public String llmDescription() {
        return "Plot a route to the mission-provider system for the active pirate-massacre missions.";
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

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> huntingGrounds = huntingGroundManager.findInProviderForTargetStarSystem(currentLocation.getStarName(), null);

        if (huntingGrounds.isEmpty()) {
            MissionManager missionManager = MissionManager.getInstance();
            Map<Long, MissionDto> missions = missionManager.getMissions(missionManager.getPirateMissionTypes());
            if (!missions.isEmpty()) {
                String targetFaction = missions.values().stream().findFirst().get().getMissionTargetFaction();
                huntingGrounds = huntingGroundManager.findInProviderForTargetStarSystem(
                        huntingGroundManager.findStarSystemForFactionName(targetFaction),
                        null
                );
            }
        }


        MissionProvider provider = null;
        String targetStarSystemName = "";
        for (PirateMissionTuple<HuntingGround, List<MissionProvider>> pair : huntingGrounds) {
            List<MissionProvider> providers = pair.getMissionProvider();

            int size = providers.size();
            // Non-terminal announcement: provider resolution below must still run, so voice the line via
            // CompanionRuntime.narrator().filler (spoken, not remembered) instead of returning here.
            if (size == 1) {
                CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.pirate.oneMissionProvider", size, pair.getTarget().getStarSystem()), false);
            } else {
                CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.pirate.manyMissionProviders", size, pair.getTarget().getStarSystem()), false);
            }

            provider = providers.stream().filter(p -> p.getMissionProviderFaction() == null).findFirst().orElse(null);
            targetStarSystemName = pair.getTarget().getStarSystem();
            if (provider != null) break;
        }

        if (provider == null) {
            if (tryConfirmedMissionProvider()) {
                return null;
            }
            return StringUtls.localizedResponse("handler.pirate.noProviderForTarget", targetStarSystemName);
        }

        huntingGrounds.stream().filter(
                data -> data.getTarget().getTargetFaction() == null
        ).findFirst().map(PirateMissionTuple::getTarget);

        String starSystem = provider.getStarSystem();
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.pirate.plottingToProvider", starSystem, targetStarSystemName), false);

        RoutePlotter plotter = new RoutePlotter();
        String result = plotter.plotRoute(starSystem);
        ReminderManager.getInstance().setReminder(
                StringUtls.localizedResponse("handler.pirate.seekProviderReminder", targetStarSystemName),
                targetStarSystemName
        );
        return result;
    }

    private boolean tryConfirmedMissionProvider() {
        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        List<MissionProvider> missionProviders = huntingGroundManager.findConfirmedMissionProviders();
        String destination = null;
        String targetSystem = null;
        for (MissionProvider provider : missionProviders) {
            if (!location.getStarName().equalsIgnoreCase(provider.getStarSystem())) {
                destination = provider.getStarSystem();
                targetSystem = provider.getTargetSystem();
                break;
            }
        }

        if (location.getStarName().equalsIgnoreCase(targetSystem)) {
            CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.pirate.checkPorts", targetSystem), false);
        } else {
            CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.pirate.headTo", destination, targetSystem), false);
        }

        if (destination == null) {
            CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.pirate.noKnowingProviders"), false);
            GameEventBus.publish(new UserInputEvent(" find hunting grounds"));
            return false;
        } else {
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(destination);
            return true;
        }

    }
}
