package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.mouth.subscribers.events.MiningAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import java.util.List;

import static elite.intel.util.StringUtls.capitalizeWords;

/**
 * Owns its own execution: body migrated 1:1 from the legacy AddMiningTargetHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class AddMiningTargetCommand implements IntelCommand {
    public static final String ID = "add_mining_target";


    private final PlayerSession playerSession = PlayerSession.getInstance();

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key", "string", true,
                "The material to add to the mining target list, e.g. platinum, painite.",
                List.of("platinum", "painite"),
                "Extract the mineral/material name verbatim in lower case.");
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
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
