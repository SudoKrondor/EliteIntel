package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.util.StringUtls;

public class SetFleetCarrierFuelReserveHandler implements CommandHandler {

    @Override public void handle(String action, JsonObject params, String responseText) {
        JsonElement key = params.get("key");
        if (key == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.invalidFuelReserve")));
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
