package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.RouteAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_JUMP_TO_HYPERSPACE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_TARGET_NEXT_ROUTE_SYSTEM;

public class JumpToHyperspaceHandler implements CommandHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();


    @Override public void handle(String action, JsonObject params, String responseText) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding())));
        UiNavCommon.close();
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.delay(150)));
        FsdTarget fsdTarget = playerSession.getFsdTarget();
        if (fsdTarget != null) {
            String starName = fsdTarget.getName() == null ? "unknown" : fsdTarget.getName();
            String fuelStatus = fsdTarget.getFuelStarStatus() == null ? "unknown" : fsdTarget.getFuelStarStatus();
            String starClass = fsdTarget.getStarClass() == null ? "unknown" : fsdTarget.getStarClass();
            EventBusManager.publish(new RouteAnnouncementEvent(StringUtls.localizedLlm("handler.fsd.jumping", starName, starClass, fuelStatus)));
        }

        Status status = Status.getInstance();

        if (status.isFsdCharging()) return;

        if (status.isFsdMassLocked()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.massLocked")));
        } else if (status.isFsdCooldown()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.cooldown")));
        } else if (status.isInMainShip()) {
            PreFtlChecks.preJumpCheck(status, StringUtls.localizedLlm("handler.supercruise.preparingFtl"));
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_JUMP_TO_HYPERSPACE.getGameBinding())));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.notInShip")));
        }
    }
}
