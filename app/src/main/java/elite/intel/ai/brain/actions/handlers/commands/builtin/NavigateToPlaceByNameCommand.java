package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * The answer to "navigate to Soul": a route cannot be plotted to a place the commander names aloud. Without a
 * tool for it, the LLM fell back to speak and improvised "please specify the system name" - an invitation to
 * repeat a request that can never succeed. This command owns that request so the reply is a fixed, truthful line.
 */
@RegisterCommand
public final class NavigateToPlaceByNameCommand implements IntelCommand {
    public static final String ID = "navigate_to_star_system_or_place_named_aloud";

    @Override
    public String llmDescription() {
        return "The commander asks to navigate, plot a route or set course to a star system, station or place by "
                + "speaking its name. Not for home, the carrier, a mission, a trade stop, the landing zone, the "
                + "clipboard or coordinates, which have their own functions.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Only explains; offered everywhere navigation is.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        return StringUtls.localizedResponse("handler.navigate.byNameUnsupported");
    }
}
