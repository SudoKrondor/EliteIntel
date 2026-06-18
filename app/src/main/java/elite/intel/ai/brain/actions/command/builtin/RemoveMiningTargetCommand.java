package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.ai.mouth.subscribers.events.MiningAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import java.util.List;

import static elite.intel.util.StringUtls.capitalizeWords;

/**
 * Owns its own execution: body migrated 1:1 from the legacy RemoveMiningTargetHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class RemoveMiningTargetCommand implements IntelCommand {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    private static final List<CustomCommandParameterSpec> PARAMETERS = buildParameters();

    private static List<CustomCommandParameterSpec> buildParameters() {
        CustomCommandParameterSpec key = new CustomCommandParameterSpec(
                "key", "string", true,
                "The material to remove from the mining target list.",
                List.of("platinum", "painite"),
                "Extract the mineral/material name verbatim in lower case.");
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return CommandIds.REMOVE_MINING_TARGET;
    }

    @Override
    public List<CustomCommandParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        JsonElement key = params.get("key");
        if (key == null) {
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
            playerSession.removeMiningTarget(target);
        }
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.mining.targetRemoved", target)));
    }
}
