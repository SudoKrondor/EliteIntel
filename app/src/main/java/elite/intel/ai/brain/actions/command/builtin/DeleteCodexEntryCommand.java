package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy DeleteCodexEntryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class DeleteCodexEntryCommand implements IntelCommand {

    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return CommandIds.DELETE_CODEX_ENTRY;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
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
