package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.SearchExclusionManager;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Puts the system the ship is in back into the searches {@link ExcludeStarSystemFromSearchesCommand} struck it from.
 * <p>
 * WHY only the current system: a system is struck off because a search named a station there that was not, and
 * the only proof it is back - a colonist rebuilt the port - is to be there and see it. "That" system, the search
 * destination, cannot name an excluded system anyway: no search offers one any more.
 * <p>
 * Not dangerous: putting a system back loses nothing, and the next search simply may offer it again.
 */
@RegisterCommand
public final class AllowStarSystemInSearchesAgainCommand implements IntelCommand {

    public static final String ID = "allow_star_system_in_searches_again";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Allow the current star system in searches again after it was excluded from them.";
    }

    /**
     * Edits what is on file and taps no game binding, so it can be said from anywhere in the system.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        String starSystem = PlayerSession.getInstance().getPrimaryStarName();
        if (starSystem == null || starSystem.isBlank()) {
            return StringUtls.localizedResponse("handler.pirate.positionUnknown");
        }
        return SearchExclusionManager.getInstance().allow(starSystem)
                ? StringUtls.localizedResponse("handler.searchExclusion.allowed", starSystem)
                : StringUtls.localizedResponse("handler.searchExclusion.notExcluded", starSystem);
    }
}
