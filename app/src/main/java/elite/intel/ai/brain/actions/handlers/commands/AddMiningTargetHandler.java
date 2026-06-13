package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MiningAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import static elite.intel.util.StringUtls.capitalizeWords;

public class AddMiningTargetHandler implements CommandHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        playerSession.setMiningAnnouncementOn(true);
        JsonElement key = params.get("key");
        if(key == null){
            EventBusManager.publish(new MiningAnnouncementEvent(StringUtls.localizedLlm("handler.mining.didNotCatch")));
            return;
        }
        String target = capitalizeWords(
                FuzzySearch.fuzzyCommodityMatch(
                                key.getAsString(), 3
                        )
                );

        if (target == null || target.isEmpty()) {
            EventBusManager.publish(new MiningAnnouncementEvent(StringUtls.localizedLlm("handler.mining.notFoundInDb", key.getAsString())));
            return;
        } else {
            playerSession.addMiningTarget(target);
        }
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.mining.targetSet", target)));
    }
}
