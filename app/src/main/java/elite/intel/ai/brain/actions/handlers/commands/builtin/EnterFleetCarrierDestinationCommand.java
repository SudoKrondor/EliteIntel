package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.hands.KeyProcessor;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.Status;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;

import java.util.Collections;
import java.util.Map;

/**
 * Owns its own execution: body migrated 1:1 from the legacy EnterNextCarrierDestinationHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class EnterFleetCarrierDestinationCommand implements IntelCommand {
    public static final String ID = "enter_fleet_carrier_destination";

    @Override
    public String llmDescription() {
        return "Type the next fleet-carrier route leg's destination system into the carrier navigation field and confirm it (used after calculating a carrier route with the carrier/galaxy map open).";
    }


    @Override
    public String id() {
        return ID;
    }

    /**
     * available anywhere. Used on fleet carrier management map which is available in any state
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
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
                GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
            }
        }
        return null;
    }
}
