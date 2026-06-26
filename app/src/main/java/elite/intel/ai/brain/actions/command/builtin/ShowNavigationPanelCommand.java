package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
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
    public static final String ID = "show_navigation_panel";

    @Override public String llmDescription() { return "Open the navigation panel."; }


    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
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
