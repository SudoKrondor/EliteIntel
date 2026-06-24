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
import elite.intel.session.ui.LeftPanel;
import elite.intel.session.ui.UINavigator;

/**
 * Stage-4b self-describing command for "request docking".
 */
@RegisterCommand
public final class RequestDockingCommand implements IntelCommand {
    public static final String ID = "request_docking";


    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if(status.isInMainShip()){
            navigator.assumeDefaultState(StatusFlags.GuiFocus.EXTERNAL_PANEL);
            // Open contacts before navigating to the station docking request.
            navigator.openAndNavigate(StatusFlags.GuiFocus.EXTERNAL_PANEL, LeftPanel.CONTACTS);
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    // Navigate within contacts to the station entry.
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.bindingHold(Bindings.GameCommand.BINDING_UI_UP.getGameBinding(), 500), // scroll up to the top (and hope our station is there)
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding()),
                    GameInputStep.delay(500),
                    // Request docking.
                    GameInputStep.bindingHold(Bindings.GameCommand.BINDING_UI_SELECT.getGameBinding(), 120)
            ));
            // Exit the panel and restore the assumed UI state.
            navigator.closeAndRestore(StatusFlags.GuiFocus.EXTERNAL_PANEL);
        }
        if (status.isInFighter()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding()),
                    GameInputStep.bindingHold(Bindings.GameCommand.BINDING_UI_SELECT.getGameBinding(), 120),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding())
            ));
        }
    }
}
