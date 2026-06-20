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
 * Self-describing "transfer power to weapons" command.
 * Owns its own execution: body migrated 1:1 from the legacy SetPowerToWeaponsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TransferPowerToWeaponsCommand implements IntelCommand {
    public static final String ID = "transfer_power_to_weapons";


    private static final Logger log = LogManager.getLogger(TransferPowerToWeaponsCommand.class);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInMainShip()) {
            powerToWeaponsShip();
        }

        if (status.isInSrv()) {
            powerToWeaponsSRV();
        }
    }

    private void powerToWeaponsSRV() {
        String resetPowerDistribution = BINDING_RESET_POWER_DISTRIBUTION_BUGGY.getGameBinding();
        String increaseWeaponsPower = BINDING_INCREASE_WEAPONS_POWER_BUGGY.getGameBinding();
        String increaseEnginesPower = BINDING_INCREASE_ENGINES_POWER_BUGGY.getGameBinding();

        performAction(resetPowerDistribution, increaseWeaponsPower, increaseEnginesPower);
    }

    private void powerToWeaponsShip() {
        String resetPowerDistribution = BINDING_RESET_POWER_DISTRIBUTION.getGameBinding();
        String increaseWeaponsPower = BINDING_INCREASE_WEAPONS_POWER.getGameBinding();
        String increaseEnginesPower = BINDING_INCREASE_ENGINES_POWER.getGameBinding();

        performAction(resetPowerDistribution, increaseWeaponsPower, increaseEnginesPower);
    }

    private void performAction(String resetPowerDistribution, String increaseWeaponsPower, String increaseEnginesPower) {
        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(resetPowerDistribution),
                GameInputStep.bindingTap(increaseWeaponsPower),
                GameInputStep.bindingTap(increaseWeaponsPower)
        ));
        log.info("Diverting power to weapons");
    }
}
