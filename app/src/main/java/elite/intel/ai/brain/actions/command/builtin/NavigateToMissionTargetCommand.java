package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Self-describing "navigate to mission target" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToMissionDestination,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToMissionTargetCommand implements IntelCommand {
    public static final String ID = "navigate_to_mission_target";

    @Override public String llmDescription() { return "Plot a route to the active mission target."; }


    private final MissionManager missionManager = MissionManager.getInstance();

    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", false,
                "Optional keyword to pick a specific mission (e.g. faction, commodity, or target name). "
                        + "If omitted, the first active mission is used.",
                List.of("massacre", "courier"),
                "Extract a distinguishing keyword from the mission the commander names; otherwise omit it.");
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        String keyword = params.get(PARAM_KEY) == null ? null : params.get(PARAM_KEY).getAsString();

        MissionDto mission = missionManager.findByKeyword(keyword).stream().findFirst().orElse(null);
        if (mission == null) {
            mission = missionManager.getMissions().values().stream().findFirst().orElse(null);
            if (mission == null) {
                GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.noMissionsFound")));
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Head to ");
        if(mission.getDestinationSystem() != null){
            sb.append(mission.getDestinationStation());
        }
        if(mission.getDestinationSettlement() != null){
            sb.append(mission.getDestinationStation());
        }

        ReminderManager.getInstance().setReminder(
                sb.toString(),
                mission.getDestinationSystem()
        );

        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.headToSystem", mission.getDestinationSystem())));
        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(mission.getDestinationSystem());
    }
}
