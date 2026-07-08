package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy DeleteCodexEntryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class DeleteCodexEntryCommand implements IntelCommand {
    public static final String ID = "delete_codex_entry";

    @Override
    public String llmDescription() {
        return "Delete the currently tracked codex / bio-sample navigation entry and stop tracking it.";
    }


    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** App-side bookkeeping (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        TargetLocation tracking = playerSession.getTracking();
        if (tracking != null) {
            codexEntryManager.deleteTrackedEntry(tracking);
            playerSession.setTracking(null);
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.codex.deleted")));
        } else {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.codex.noTracking")));
        }
    }
}
