package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SetFleetCarrierFuelReserveHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SetCarrierFuelReserveCommand implements IntelCommand {
    public static final String ID = "set_carrier_fuel_reserve";

    @Override public String llmDescription() { return "Set the fleet carrier's tritium fuel reserve level."; }


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        // Handler reads this value under the key "key" (getAsString -> getIntSafely);
        // type=number is safe because the read is string-based.
        ActionParameterSpec key = new ActionParameterSpec(
                "key", "number", true,
                "Fleet carrier tritium fuel reserve amount to set.",
                List.of("500", "1000"),
                "Extract the numeric tritium reserve amount the commander specifies.");
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
    public JsonObject execute(JsonObject params, String responseText) {
        JsonElement key = params.get("key");
        if (key == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.fleetCarrier.invalidFuelReserve"));
        }
        Integer reserve = StringUtls.getIntSafely(key.getAsString());
        if (reserve == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.fleetCarrier.invalidFuelReserve"));
        }
        FleetCarrierManager fleetCarrierManager = FleetCarrierManager.getInstance();
        CarrierDataDto dto = fleetCarrierManager.get();
        dto.setFuelReserve(reserve);
        fleetCarrierManager.save(dto);
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.fleetCarrier.fuelReserveSet", reserve));
    }
}
