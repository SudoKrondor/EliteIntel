package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.CommsPanel;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

/**
 * Stage-4b self-describing command for "show history panel".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy OpenHistoryPanelHandler.
 */
@RegisterCommand
public final class ShowHistoryPanelCommand implements IntelCommand {

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return CommandIds.SHOW_HISTORY_PANEL;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (status.isInMainShip() || status.isInFighter()) {
            navigator.openAndNavigate(StatusFlags.GuiFocus.COMMS_PANEL, CommsPanel.HISTORY);
        } else if (status.isOnFoot()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingHold(BINDING_ON_FOOT_WHEEL.getGameBinding(), 500),
                    GameInputStep.delay(500),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_CYCLE_NEXT_PANEL.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_CYCLE_NEXT_PANEL.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_CYCLE_NEXT_PANEL.getGameBinding())
            ));
        } else {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.common.cantDoNow")));
        }
    }
}
