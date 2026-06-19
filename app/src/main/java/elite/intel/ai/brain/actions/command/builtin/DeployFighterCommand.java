package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage-4b self-describing command for "deploy fighter".
 */
@RegisterCommand
public final class DeployFighterCommand implements IntelCommand {
    public static final String ID = "deploy_fighter";


    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (status.isInMainShip()) {
            List<GameInputStep> steps = new ArrayList<>();
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding()));
            // Ensure the cursor is at the top before navigating to deploy fighter.
            for (int i = 0; i < 5; i++) {
                steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()));
            }
            for (int i = 0; i < 5; i++) {
                steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()));
            }

            // Deploy Fighter.
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()));
            steps.add(GameInputStep.delay(150));
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding()));
            steps.add(GameInputStep.delay(150));
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_SELECT.getGameBinding()));
            for (int i = 0; i < 6; i++) {
                steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()));
                steps.add(GameInputStep.delay(150));
            }
            for (int i = 0; i < 3; i++) {
                steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()));
                steps.add(GameInputStep.delay(150));
            }
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_SELECT.getGameBinding()));
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()));
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()));
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding()));
            GameControllerBus.publish(new GameInputSequenceEvent(steps));
            status.setOkToAnnounceLoadout(false);
        }
    }
}
