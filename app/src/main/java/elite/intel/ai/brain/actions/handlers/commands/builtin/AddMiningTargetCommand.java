package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.FuzzySearch;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
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

    @Override
    public String llmDescription() {
        return "Add a mineable commodity (in 'key', e.g. Platinum, Painite) to the mining prospector target list so it is called out while prospecting.";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();

    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
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

    /** App-side bookkeeping (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        playerSession.setMiningAnnouncementOn(true);
        JsonElement key = params.get(PARAM_KEY);
        if(key == null){
            return StringUtls.localizedLlm("handler.mining.didNotCatch");
        }
        String target = capitalizeWords(
                FuzzySearch.fuzzyCommodityMatch(
                                key.getAsString(), 3
                        )
                );

        if (target == null || target.isEmpty()) {
            return StringUtls.localizedLlm("handler.mining.notFoundInDb", key.getAsString());
        }

        playerSession.addMiningTarget(target);
        return StringUtls.localizedLlm("handler.mining.targetSet", target);
    }
}
