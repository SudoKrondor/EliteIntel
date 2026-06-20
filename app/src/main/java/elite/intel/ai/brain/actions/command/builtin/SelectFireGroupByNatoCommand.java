package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.gameapi.FireGroups;

import java.util.List;

import static elite.intel.gameapi.FireGroups.fireGroupByNato;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SelectFireGroupByNatoHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SelectFireGroupByNatoCommand implements IntelCommand {
    public static final String ID = "select_fire_group_by_nato";


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key", "string", true,
                "The fire group identified by its NATO phonetic word (alpha, bravo, charlie, ...).",
                List.of("alpha", "charlie"),
                "Extract the NATO phonetic word verbatim in lower case; do NOT convert it to a letter.");
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

        JsonElement key = params.get("key");
        if (key == null) {
            return;
        }

        String nato = key.getAsString();
        if (nato == null) {
            return;
        }

        int fireGroupInSettings = fireGroupByNato(nato);
        if (fireGroupInSettings == -1) return;

        FireGroups.cycleToGroup(fireGroupInSettings);
    }
}
