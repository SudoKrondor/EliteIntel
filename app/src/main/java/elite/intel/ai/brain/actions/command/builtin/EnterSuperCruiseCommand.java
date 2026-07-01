package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.PreFtlChecks;
import elite.intel.gameapi.inputs.UiNavCommon;
import elite.intel.session.Status;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

/**
 * Self-describing "enter super cruise" command.
 * Owns its own execution: body migrated 1:1 from the legacy SuperCruiseHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class EnterSuperCruiseCommand implements IntelCommand {
    public static final String ID = "enter_super_cruise";

    @Override public String llmDescription() { return "Engage supercruise."; }


    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();
    private final GlobalSettingsManager settingsManager = GlobalSettingsManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        UiNavCommon.close();

        if (status.isFsdCharging()) return;

        if (status.isFsdMassLocked()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.massLocked")));
        } else if (status.isFsdCooldown()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.cooldown")));
        }

        ///NOTE. this is commented out until FDev fixes the Status.json.
        /// Game has a bug status.isFighterOut() == true when nomad is equipped and returned to base.
//        else if (status.isFighterOut()) {
//            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_REQUEST_REQUEST_DOCK.getGameBinding())));
//            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.fighterOut")));
//        }

        else if (status.isInMainShip()) {
            if (status.isInSupercruise()) {
                navigator.closeOpenPanel();
                if (settingsManager.getAutoSpeedUpForFtl()) {
                    GameControllerBus.publish(GameInputSequenceEvent.of(
                            GameInputStep.bindingTap(BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding()),
                            GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding()),
                            GameInputStep.bindingTap(BINDING_JUMP_TO_HYPERSPACE.getGameBinding()),
                            GameInputStep.delay(12_000),
                            GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding())
                    ));
                } else {
                    GameControllerBus.publish(GameInputSequenceEvent.of(
                            GameInputStep.bindingTap(BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding()),
                            GameInputStep.bindingTap(BINDING_JUMP_TO_HYPERSPACE.getGameBinding())
                    ));
                }
            } else {
                PreFtlChecks.preJumpCheck(status, StringUtls.localizedLlm("handler.supercruise.preparing"));
                if (settingsManager.getAutoSpeedUpForFtl()) {
                    GameControllerBus.publish(GameInputSequenceEvent.of(
                            GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding()),
                            GameInputStep.bindingTap(BINDING_ENTER_SUPERCRUISE.getGameBinding()),
                            GameInputStep.delay(1_000),
                            GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding()),
                            GameInputStep.delay(1_000),
                            GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding()),
                            GameInputStep.delay(1_000),
                            GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding()),
                            GameInputStep.delay(1_000),
                            GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding()),
                            GameInputStep.delay(1_000),
                            GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding())
                    ));
                } else {
                    GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ENTER_SUPERCRUISE.getGameBinding())));
                }
            }
        } else {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.notInShip")));
        }
    }
}
