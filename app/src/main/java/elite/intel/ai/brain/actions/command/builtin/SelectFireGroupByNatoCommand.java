package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.gameapi.FireGroups;

import java.util.List;

import static elite.intel.gameapi.FireGroups.fireGroupByNato;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SelectFireGroupByNatoHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SelectFireGroupByNatoCommand implements IntelCommand {

    private static final List<CustomCommandParameterSpec> PARAMETERS = buildParameters();

    private static List<CustomCommandParameterSpec> buildParameters() {
        CustomCommandParameterSpec key = new CustomCommandParameterSpec(
                "key", "string", true,
                "The fire group identified by its NATO phonetic word (alpha, bravo, charlie, ...).",
                List.of("alpha", "charlie"),
                "Extract the NATO phonetic word verbatim in lower case; do NOT convert it to a letter.");
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return CommandIds.SELECT_FIRE_GROUP_BY_NATO;
    }

    @Override
    public List<CustomCommandParameterSpec> parameters() {
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
