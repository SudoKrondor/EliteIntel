package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.db.managers.SubSystemsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;

/**
 * Owns its own execution: body migrated 1:1 from the legacy TargetSubSystemHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TargetSubsystemCommand implements IntelCommand {
    public static final String ID = "target_subsystem";


    private static final Logger log = LogManager.getLogger(TargetSubsystemCommand.class);

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key", "string", true,
                "The ship subsystem to target, e.g. fsd, drive, power distributor, powerplant, life support.",
                List.of("fsd", "power distributor"),
                "Extract the subsystem name verbatim in lower case (e.g. 'target drive' -> drive).");
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
        log.debug("TargetSubSystemHandler received params: {}", params);
        JsonElement key = params.get("key");

        String subSystem;
        if (key == null) {
            log.debug("No 'key' param from LLM - defaulting to power plant");
            subSystem = "power plant";
        } else {
            subSystem = key.getAsString()
                    .toLowerCase(Locale.ROOT)
                    .replace(".", "")
                    .replace(",", "")
                    .replace("frame shift drive", "fsd")
                    .replace("thrusters", "drive")
                    .replace("engines", "drive")
                    .replace("shields", "shield generator")
                    .replace("powerplant", "power plant")
                    .trim();
            log.debug("LLM key=[{}] normalized to=[{}]", key.getAsString(), subSystem);
        }
        SubSystemsManager.getInstance().targetSubSystem(subSystem);
    }
}
