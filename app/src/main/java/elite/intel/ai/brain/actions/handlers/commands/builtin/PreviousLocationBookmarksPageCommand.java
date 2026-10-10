package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.ui.overlay.LocationBookmarkCard;
import elite.intel.util.StringUtls;

/**
 * "Previous bookmark page" / "scroll bookmarks up". Silent when the page turns - the card is the answer.
 */
@RegisterCommand
public final class PreviousLocationBookmarksPageCommand implements IntelCommand {
    public static final String ID = "previous_location_bookmarks_page";

    @Override
    public String llmDescription() {
        return "Show the previous page of location bookmarks on the overlay (scroll the bookmark list up).";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Only while the bookmark card is on screen: there is no page to turn otherwise.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return LocationBookmarkCard.getInstance().isOpen();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        LocationBookmarkCard card = LocationBookmarkCard.getInstance();
        if (!card.isOpen()) return StringUtls.localizedResponse("handler.bookmark.notOpen");
        return card.previousPage() == LocationBookmarkCard.Turn.AT_END
                ? StringUtls.localizedResponse("handler.bookmark.firstPage")
                : null;
    }
}
