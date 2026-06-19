package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.UiNavCommon;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.UINavigator;

/**
 * Self-describing "disembark" command.
 * Owns its own execution: body migrated 1:1 from the legacy DisembarkHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class DisembarkCommand implements IntelCommand {

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return CommandIds.DISEMBARK;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        UiNavCommon.close();
        if (status.isInSrv()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding()),
                    // Ensure the cursor is at the top before navigating to disembark.
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    // Disembark.
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding())
            ));
            navigator.assumeDefaultState(StatusFlags.GuiFocus.ROLE_PANEL);
        } else if (status.isInMainShip()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding())
            ));
        }
    }
}
