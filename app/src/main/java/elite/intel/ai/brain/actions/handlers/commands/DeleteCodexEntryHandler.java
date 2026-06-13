package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

public class DeleteCodexEntryHandler implements CommandHandler {

    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override public void handle(String action, JsonObject params, String responseText) {
        TargetLocation tracking = playerSession.getTracking();
        if (tracking != null) {
            codexEntryManager.deleteTrackedEntry(tracking);
            playerSession.setTracking(null);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.codex.deleted")));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.codex.noTracking")));
        }
    }
}
