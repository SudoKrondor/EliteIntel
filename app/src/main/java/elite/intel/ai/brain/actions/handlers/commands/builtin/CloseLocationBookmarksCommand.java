package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.ui.overlay.LocationBookmarkCard;

/**
 * "Close bookmarks" - takes the list down, and the overlay goes back to whatever card the ranking picks.
 */
@RegisterCommand
public final class CloseLocationBookmarksCommand implements IntelCommand {
    public static final String ID = "close_location_bookmarks";

    @Override
    public String llmDescription() {
        return "Close the location bookmark list on the overlay.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Only while the bookmark card is on screen.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return LocationBookmarkCard.getInstance().isOpen();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        LocationBookmarkCard.getInstance().close();
        return null;
    }
}
