package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.gameapi.FireGroups;
import elite.intel.session.Status;

import java.util.List;

import static elite.intel.gameapi.FireGroups.fireGroupByNato;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SelectFireGroupByNatoHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SelectFireGroupByNatoCommand implements IntelCommand {
    public static final String ID = "select_fire_group_by_nato";

    @Override
    public String llmDescription() {
        return "Switch to the weapon fire group named by the NATO-alphabet letter in 'key' (Alpha = first group, Bravo = second, etc.).";
    }


    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
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

    /** Fire groups exist only while piloting a vehicle with modules: main ship, fighter, or SRV. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() || status.isInFighter() || status.isInSrv();
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {

        JsonElement key = params.get(PARAM_KEY);
        if (key == null) {
            return null;
        }

        String nato = key.getAsString();
        if (nato == null) {
            return null;
        }

        int fireGroupInSettings = fireGroupByNato(nato);
        if (fireGroupInSettings == -1) return null;

        FireGroups.cycleToGroup(fireGroupInSettings);
        return null;
    }
}
