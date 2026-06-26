package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
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

    @Override public String llmDescription() { return "Plot a route to the mission provider."; }


    private final HuntingGroundManager huntingGroundManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {

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
            provider = providers.stream().filter(p -> p.getMissionProviderFaction() == null).findFirst().orElse(null);
            targetStarSystemName = pair.getTarget().getStarSystem();
            if (provider != null) break;
        }

        if (provider == null) {
            JsonObject confirmed = tryConfirmedMissionProvider();
            if (confirmed != null) {
                return confirmed;
            }
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.pirate.noProviderForTarget", targetStarSystemName));
        }

        String starSystem = provider.getStarSystem();
        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(starSystem);
        ReminderManager.getInstance().setReminder(
                StringUtls.localizedLlm("handler.pirate.seekProviderReminder", targetStarSystemName),
                targetStarSystemName
        );
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.pirate.plottingToProvider", starSystem, targetStarSystemName));
    }

    /**
     * Tries to route to a known confirmed mission provider. Returns the spoken outcome when one is found
     * (and plots the route), or {@code null} when none is known - in which case it triggers a hunting-ground
     * search as a side-effect and lets the caller supply the "no provider" outcome.
     */
    private JsonObject tryConfirmedMissionProvider() {
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

        if (destination == null) {
            // No known provider elsewhere: kick off a hunting-ground search and defer the outcome to the caller.
            GameEventBus.publish(new UserInputEvent(" find hunting grounds"));
            return null;
        }

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(destination);
        String message = location.getStarName().equalsIgnoreCase(targetSystem)
                ? StringUtls.localizedLlm("handler.pirate.checkPorts", targetSystem)
                : StringUtls.localizedLlm("handler.pirate.headTo", destination, targetSystem);
        return CommandOutcome.critical(message);
    }
}
