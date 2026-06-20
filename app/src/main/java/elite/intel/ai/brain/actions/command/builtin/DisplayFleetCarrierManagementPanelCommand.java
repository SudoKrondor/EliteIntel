package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.gameapi.inputs.UiNavCommon;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

/**
 * Self-describing "display fleet carrier management panel" command.
 * Owns its own execution: body migrated 1:1 from the legacy OpenFleetCarrierManagementHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class DisplayFleetCarrierManagementPanelCommand implements IntelCommand {
    public static final String ID = "display_fleet_carrier_management_panel";


    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /// not a sure fire. assumes default UI selection. will fail often.
    @Override
    public void execute(JsonObject params, String responseText) {
        UiNavCommon.close();
        if (status.isOnFoot()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingHold(BINDING_ON_FOOT_WHEEL.getGameBinding(), 500),
                    GameInputStep.bindingTap(BINDING_UI_RIGHT.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding())
            ));
        } else if (status.isInMainShip()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(BINDING_FOCUS_INTERNAL_PANEL.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.delay(100),
                    GameInputStep.bindingTap(BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.delay(100),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.delay(100),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.delay(100),
                    GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.delay(100),
                    GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding())
            ));
        } else if (status.isInSrv()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(BINDING_FOCUS_INTERNAL_PANEL_BUGGY.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding())
            ));
        }
    }
}
