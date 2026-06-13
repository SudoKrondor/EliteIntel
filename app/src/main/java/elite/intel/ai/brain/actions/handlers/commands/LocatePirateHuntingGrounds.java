package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.PirateHuntingGroundsDao.HuntingGround;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager.PirateMissionTuple;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.missions.pirates.PirateMassacreMissionSearch;
import elite.intel.util.StringUtls;

import java.util.List;

import static elite.intel.util.StringUtls.getIntSafely;

public class LocatePirateHuntingGrounds implements CommandHandler {

    /**
     * No Recon
     * Will query the local database first. If nothing is found will call external API
     */
    @Override public void handle(String action, JsonObject params, String responseText) {
        int range = params.get("key") == null
                || getIntSafely(params.get("key").getAsString()) == null
                || params.isEmpty() ? 100 : params.get("key").getAsInt();

        PirateMassacreMissionSearch missionSearch = PirateMassacreMissionSearch.getInstance();
        List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> huntingGrounds = missionSearch.findHuntingSpotsInRange(range);

        if (huntingGrounds != null) {
            String message;
            if (huntingGrounds.isEmpty()) {
                message = StringUtls.localizedLlm("handler.pirate.noProviders");
            } else {
                String providers = huntingGrounds.size() == 1
                        ? StringUtls.localizedLlm("handler.pirate.foundProvidersOne")
                        : StringUtls.localizedLlm("handler.pirate.foundProvidersMany", huntingGrounds.size());
                boolean reconRequired = huntingGrounds.stream().anyMatch(data -> !data.getTarget().isHasResSite());
                String nav = reconRequired
                        ? StringUtls.localizedLlm("handler.pirate.reconRequired")
                        : StringUtls.localizedLlm("handler.pirate.askMissionProvider");
                message = providers + " " + nav;
            }
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(message));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.noHuntingGrounds", range)));
        }
    }
}
