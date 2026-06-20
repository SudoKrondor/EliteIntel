package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.RouteAnnouncementEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.inputs.PreFtlChecks;
import elite.intel.gameapi.inputs.UiNavCommon;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_JUMP_TO_HYPERSPACE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_TARGET_NEXT_ROUTE_SYSTEM;

/**
 * Self-describing "jump to hyperspace" command.
 * Owns its own execution: body migrated 1:1 from the legacy JumpToHyperspaceHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class JumpToHyperspaceCommand implements IntelCommand {
    public static final String ID = "jump_to_hyperspace";


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding())));
        UiNavCommon.close();
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.delay(150)));
        FsdTarget fsdTarget = playerSession.getFsdTarget();
        if (fsdTarget != null) {
            String starName = fsdTarget.getName() == null ? "unknown" : fsdTarget.getName();
            String fuelStatus = fsdTarget.getFuelStarStatus() == null ? "unknown" : fsdTarget.getFuelStarStatus();
            String starClass = fsdTarget.getStarClass() == null ? "unknown" : fsdTarget.getStarClass();
            GameEventBus.publish(new RouteAnnouncementEvent(StringUtls.localizedLlm("handler.fsd.jumping", starName, starClass, fuelStatus)));
        }

        Status status = Status.getInstance();

        if (status.isFsdCharging()) return;

        if (status.isFsdMassLocked()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.massLocked")));
        } else if (status.isFsdCooldown()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.cooldown")));
        } else if (status.isInMainShip()) {
            PreFtlChecks.preJumpCheck(status, StringUtls.localizedLlm("handler.supercruise.preparingFtl"));
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_JUMP_TO_HYPERSPACE.getGameBinding())));
        } else {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.notInShip")));
        }
    }
}
