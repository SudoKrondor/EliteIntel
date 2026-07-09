package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.UINavigator;

/**
 * Stage-4b self-describing command for "deploy srv".
 */
@RegisterCommand
public final class DeployVehicleSrvCommand implements IntelCommand {
    public static final String ID = "deploy_vehicle_srv";

    @Override
    public String llmDescription() {
        return "Deploy the SRV surface buggy from the ship's vehicle hangar; only while the main ship is landed on a planet surface. Not a ship undock, fighter, or Nomad.";
    }


    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    /**
     * The SRV can only leave the bay when the ship is landed on a planetary surface, so it is offered only in that
     * context. This keeps "launch/deploy SRV" from competing with undocking (launch ship), which is a dock-only
     * command - the game-state gate the legacy path relied on.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && status.isLanded();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        if (status.isInMainShip()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding()),
                    // Ensure the cursor is at the top before navigating to the SRV option.
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    // Deploy SRV.
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding())
            ));
            navigator.assumeDefaultState(StatusFlags.GuiFocus.ROLE_PANEL);
        }
        return null;
    }
}
