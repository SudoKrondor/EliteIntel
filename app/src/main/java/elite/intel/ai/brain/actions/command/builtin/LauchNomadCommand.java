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

@RegisterCommand
public class LauchNomadCommand implements IntelCommand {

    public static final String ID = "lauch_deploy_nomad";

    @Override
    public String llmDescription() {
        return "Deploy the Nomad aerial scout from the ship while flying low over a planet surface; not a ship undock, SRV, or fighter.";
    }

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    /**
     * The Nomad is an AERIAL scout (no wheels), deployed from the ship while in flight - not in supercruise, and
     * not parked (docked/landed). Gating to that context keeps it distinct from the ground-only SRV and the
     * dock-only undock (launch ship). Note the FDev quirk: once out, the Nomad reports as an SRV
     * ({@code isInSrv()} is true), so this deploy command deliberately keys off {@code isInMainShip()} - which is
     * false while piloting the Nomad - to stop offering itself once the Nomad is already out.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip()
                && !status.isDocked()
                && !status.isLanded()
                && !status.isInSupercruise();
    }


    @Override
    public void execute(JsonObject params, String responseText) {
        if (status.isInMainShip()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding()),
                    // Ensure the cursor is at the top before navigating to the SRV option.
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                    // Deploy Nomad.
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding()),
                    GameInputStep.bindingTap(Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding())
            ));
            navigator.assumeDefaultState(StatusFlags.GuiFocus.ROLE_PANEL);

        }
    }

    @Override
    public String id() {
        return ID;
    }
}
