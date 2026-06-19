package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.PreFtlChecks;
import elite.intel.ai.hands.UiNavCommon;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
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

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();
    private final GlobalSettingsManager settingsManager = GlobalSettingsManager.getInstance();

    @Override
    public String id() {
        return CommandIds.ENTER_SUPER_CRUISE;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        UiNavCommon.close();

        if (status.isFsdCharging()) return;

        if (status.isFsdMassLocked()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.massLocked")));
        } else if (status.isFsdCooldown()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.cooldown")));
        } else if (status.isFighterOut()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_REQUEST_REQUEST_DOCK.getGameBinding())));
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.fighterOut")));
        } else if (status.isInMainShip()) {
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.notInShip")));
        }
    }
}
