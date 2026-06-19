package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy SetFleetCarrierFuelReserveHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class SetCarrierFuelReserveCommand implements IntelCommand {

    private static final List<CustomCommandParameterSpec> PARAMETERS = buildParameters();

    private static List<CustomCommandParameterSpec> buildParameters() {
        // Handler reads this value under the key "key" (getAsString -> getIntSafely);
        // type=number is safe because the read is string-based.
        CustomCommandParameterSpec key = new CustomCommandParameterSpec(
                "key", "number", true,
                "Fleet carrier tritium fuel reserve amount to set.",
                List.of("500", "1000"),
                "Extract the numeric tritium reserve amount the commander specifies.");
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return CommandIds.SET_CARRIER_FUEL_RESERVE;
    }

    @Override
    public List<CustomCommandParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        JsonElement key = params.get("key");
        if (key == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.invalidFuelReserve")));
            return;
        }
        Integer reserve = StringUtls.getIntSafely(key.getAsString());
        if(reserve == null){
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.invalidFuelReserve")));
            return;
        }
        FleetCarrierManager fleetCarrierManager = FleetCarrierManager.getInstance();
        CarrierDataDto dto = fleetCarrierManager.get();
        dto.setFuelReserve(reserve);
        fleetCarrierManager.save(dto);
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.fuelReserveSet", reserve)));
    }
}
