package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.MissionManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.StringUtls;

import java.util.Set;

/**
 * Self-describing "navigate to pirate mission target" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToPirateMassacreMissionTargetHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToPirateMissionTargetCommand implements IntelCommand {
    public static final String ID = "navigate_to_pirate_mission_target";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        MissionManager missionManager = MissionManager.getInstance();

        MissionType[] missionTypes = missionManager.getPirateMissionTypes();
        Set<String> targetFactions = missionManager.getTargetFactions(missionTypes);

        if (targetFactions.isEmpty()) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.pirate.noProvidersMassacre")));
            return;
        }

        MissionDto mission = missionManager.getMissions(missionTypes)
                .values()
                .stream().filter(v -> v.getMissionType().equals(MissionType.MISSION_PIRATE_MASSACRE)).findFirst().orElse(null);

        if(mission == null) return;

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(mission.getDestinationSystem());
    }
}
