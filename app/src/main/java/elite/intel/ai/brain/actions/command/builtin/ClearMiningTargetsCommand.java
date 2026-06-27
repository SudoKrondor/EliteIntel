package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ClearMiningTargetsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ClearMiningTargetsCommand implements IntelCommand {
    public static final String ID = "clear_mining_targets";

    @Override public String llmDescription() { return "Clear the mining target list."; }


    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        playerSession.clearMiningTargets();
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.mining.targetsCleared")));
        playerSession.setMiningAnnouncementOn(true);
    }
}
