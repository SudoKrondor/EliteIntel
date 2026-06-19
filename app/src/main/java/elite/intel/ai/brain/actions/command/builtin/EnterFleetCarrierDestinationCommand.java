package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.KeyProcessor;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import elite.intel.util.AudioPlayer;

import java.util.Collections;
import java.util.Map;

/**
 * Owns its own execution: body migrated 1:1 from the legacy EnterNextCarrierDestinationHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class EnterFleetCarrierDestinationCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.ENTER_FLEET_CARRIER_DESTINATION;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Map<Integer, CarrierJump> fleetCarrierRoute = FleetCarrierRouteManager.getInstance().getFleetCarrierRoute();

        if (!fleetCarrierRoute.isEmpty()) {
            Integer nextLeg = Collections.min(fleetCarrierRoute.keySet());
            CarrierJump carrierJump = fleetCarrierRoute.get(nextLeg);
            if(carrierJump.getSystemName() != null) {
                GameControllerBus.publish(GameInputSequenceEvent.of(
                        GameInputStep.text(carrierJump.getSystemName()),
                        GameInputStep.delay(250),
                        GameInputStep.rawKey(KeyProcessor.KEY_ENTER)
                ));
                AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_2);
            }
        }
    }
}
