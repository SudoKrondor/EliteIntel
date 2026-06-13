package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.StringUtls;

public class NavigateToMissionDestination implements CommandHandler {

    private final MissionManager missionManager = MissionManager.getInstance();


    @Override
    public void handle(String action, JsonObject params, String responseText) {
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
