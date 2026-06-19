package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

/**
 * Self-describing "transfer power to engines" command.
 * Owns its own execution: body migrated 1:1 from the legacy SetPowerToEnginesHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TransferPowerToEnginesCommand implements IntelCommand {

    private static final Logger log = LogManager.getLogger(TransferPowerToEnginesCommand.class);

    @Override
    public String id() {
        return CommandIds.TRANSFER_POWER_TO_ENGINES;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (status.isInMainShip()) {
            powerToEnginesShip();
        }

        if (status.isInSrv()) {
            powerToEnginesSRV();
        }

    }

    private void powerToEnginesSRV() {
        String resetPowerDistribution = BINDING_RESET_POWER_DISTRIBUTION_BUGGY.getGameBinding();
        String increaseEnginePower = BINDING_INCREASE_ENGINES_POWER_BUGGY.getGameBinding();
        String increaseSystemPower = BINDING_INCREASE_SYSTEMS_POWER_BUGGY.getGameBinding();

        performAction(resetPowerDistribution, increaseEnginePower, increaseSystemPower);

    }

    private void powerToEnginesShip() {
        String resetPowerDistribution = BINDING_RESET_POWER_DISTRIBUTION.getGameBinding();
        String increaseEnginePower = BINDING_INCREASE_ENGINES_POWER.getGameBinding();
        String increaseSystemPower = BINDING_INCREASE_SYSTEMS_POWER.getGameBinding();

        performAction(resetPowerDistribution, increaseEnginePower, increaseSystemPower);
    }

    private void performAction(String resetPowerDistribution, String increaseEnginePower, String increaseSystemPower) {
        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(resetPowerDistribution),
                GameInputStep.bindingTap(increaseEnginePower),
                GameInputStep.bindingTap(increaseEnginePower)
        ));
        log.info("Diverting power to engines");
    }
}
