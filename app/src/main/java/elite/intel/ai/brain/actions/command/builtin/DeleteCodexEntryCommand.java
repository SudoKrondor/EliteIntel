package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy DeleteCodexEntryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class DeleteCodexEntryCommand implements IntelCommand {
    public static final String ID = "delete_codex_entry";

    @Override public String llmDescription() { return "Delete a saved codex/navigation entry."; }


    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        TargetLocation tracking = playerSession.getTracking();
        if (tracking == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.codex.noTracking"));
        }
        codexEntryManager.deleteTrackedEntry(tracking);
        playerSession.setTracking(null);
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.codex.deleted"));
    }
}
