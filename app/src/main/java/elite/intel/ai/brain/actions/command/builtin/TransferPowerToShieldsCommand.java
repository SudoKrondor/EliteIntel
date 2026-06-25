package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

/**
 * Self-describing "transfer power to shields" command.
 * Owns its own execution: body migrated 1:1 from the legacy SetPowerToSystemsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TransferPowerToShieldsCommand implements IntelCommand {
    public static final String ID = "transfer_power_to_shields";

    @Override public String llmDescription() { return "Divert power to shields (systems)."; }


    private static final Logger log = LogManager.getLogger(TransferPowerToShieldsCommand.class);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInMainShip()) {
            powerToSystemsShip();
        }

        if (status.isInSrv()) {
            powerToSystemsSRV();
        }

        return null;
    }

    private void powerToSystemsSRV() {
        performOperation(
                BINDING_RESET_POWER_DISTRIBUTION_BUGGY.getGameBinding(),
                BINDING_INCREASE_SYSTEMS_POWER_BUGGY.getGameBinding());
    }

    private void powerToSystemsShip() {
        performOperation(
                BINDING_RESET_POWER_DISTRIBUTION.getGameBinding(),
                BINDING_INCREASE_SYSTEMS_POWER.getGameBinding());
    }

    private void performOperation(String resetPowerDistribution, String increaseSystemsPower) {
        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(resetPowerDistribution),
                GameInputStep.bindingTap(increaseSystemsPower),
                GameInputStep.bindingTap(increaseSystemsPower)
        ));
        log.info("Power distribution complete");
    }
}
