package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.StringUtls;

/**
 * Self-describing "navigate to mission target" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToMissionDestination,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToMissionTargetCommand implements IntelCommand {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Override
    public String id() {
        return CommandIds.NAVIGATE_TO_MISSION_TARGET;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        String keyword = params.get("key") == null ? null : params.get("key").getAsString();

        MissionDto mission = missionManager.findByKeyword(keyword).stream().findFirst().orElse(null);
        if (mission == null) {
            mission = missionManager.getMissions().values().stream().findFirst().orElse(null);
            if (mission == null) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.noMissionsFound")));
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

        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.headToSystem", mission.getDestinationSystem())));
        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(mission.getDestinationSystem());
    }
}
