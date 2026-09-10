package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Drops the current system as a hunting ground, and the contracts that led here with it.
 * <p>
 * WHY the commander has to be able to say this: the journal records that a system has resource
 * extraction sites, and nothing else. It does not record that the ring is an hour out from the star,
 * that the spawns are thin, or that the pirates flying there carry nothing worth shooting. Only the
 * commander who just flew it knows that, and this is how they say so.
 */
@RegisterCommand
public final class ForgetHuntingGroundCommand implements IntelCommand {

    public static final String ID = "forget_hunting_ground";

    private final HuntingGroundManager huntingGrounds = HuntingGroundManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Forget the current star system as a pirate hunting ground, so it is never recommended again, "
                + "and discard the recorded mission-provider pairs that targeted it.";
    }

    /**
     * Edits what is on file and taps no game binding, so the verdict can be given from anywhere in the system.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        String starSystem = playerSession.getPrimaryStarName();
        if (starSystem == null || starSystem.isBlank()) {
            return StringUtls.localizedResponse("handler.pirate.positionUnknown");
        }

        HuntingGroundManager.ForgetResult result = huntingGrounds.forget(starSystem);
        if (!result.wasKnown()) {
            return StringUtls.localizedResponse("handler.pirate.nothingToForget", starSystem);
        }
        if (result.contractsForgotten() == 0) {
            return StringUtls.localizedResponse("handler.pirate.groundForgotten", starSystem);
        }
        return StringUtls.localizedResponse("handler.pirate.groundAndPairsForgotten",
                starSystem, result.contractsForgotten());
    }
}
