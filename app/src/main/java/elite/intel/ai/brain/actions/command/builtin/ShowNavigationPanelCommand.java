package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.LeftPanel;
import elite.intel.session.ui.UINavigator;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

/**
 * Stage-4b self-describing command for "show navigation panel".
 */
@RegisterCommand
public final class ShowNavigationPanelCommand implements IntelCommand {

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return CommandIds.SHOW_NAVIGATION_PANEL;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (status.isInMainShip() || status.isInSrv() || status.isInFighter()) {
            navigator.openAndNavigate(StatusFlags.GuiFocus.EXTERNAL_PANEL, LeftPanel.NAVIGATION);
        } else if (status.isOnFoot()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingHold(BINDING_ON_FOOT_WHEEL.getGameBinding(), 500),
                    GameInputStep.delay(500),
                    GameInputStep.bindingTap(BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding())
            ));
        }
    }
}
