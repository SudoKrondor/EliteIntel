package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class FighterAttackTargetCommand extends SimpleTapCommand {
    public static final String ID = "fighter_attack_target";

    @Override
    public String llmDescription() {
        // WHY: opening fire is not reversible. A fighter pointed at a clean or allied ship earns the commander
        // a bounty they then have to fly somewhere and pay off, so this one asks the model for an explicit
        // order rather than a plausible one - anything short of "attack my target" is another fighter command.
        return "Order the deployed ship-launched fighter (or NPC crew) to open fire on the commander's current "
                + "target. Use ONLY when the commander explicitly ordered an attack on their target; never as "
                + "the closest guess for an unclear or garbled utterance, and never for a fighter order that "
                + "does not name attacking the target (recall, defend, hold fire, deploy).";
    }

    public FighterAttackTargetCommand() {
        super(ID, Bindings.GameCommand.BINDING_REQUEST_FOCUS_TARGET.getGameBinding());
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isFighterOut();
    }
}
