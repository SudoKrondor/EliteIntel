package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.LocationBookmarkManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.bookmarks.BookmarkSpot;
import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.session.Status;
import elite.intel.ui.event.LocationBookmarksChangedEvent;
import elite.intel.util.StringUtls;

import java.util.Optional;

/**
 * "Bookmark this" - remembers where the commander is, as precisely as the situation allows (see
 * {@link BookmarkSpot}). A new bookmark goes to the bottom of the list.
 */
@RegisterCommand
public final class BookmarkCurrentLocationCommand implements IntelCommand {
    public static final String ID = "bookmark_current_location";

    @Override
    public String llmDescription() {
        return "Save the commander's current location (system, station, planet or surface spot) as a bookmark to navigate back to later.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * App-side bookkeeping (no game input); a place can be bookmarked anywhere.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Optional<LocationBookmark> here = BookmarkSpot.current();
        if (here.isEmpty()) return StringUtls.localizedResponse("handler.bookmark.whereUnknown");

        LocationBookmarkManager.Saved saved = LocationBookmarkManager.getInstance().save(here.get());
        if (!saved.alreadyBookmarked()) UiBus.publish(new LocationBookmarksChangedEvent());
        return saved.alreadyBookmarked()
                ? StringUtls.localizedResponse("handler.bookmark.alreadySaved", String.valueOf(saved.number()))
                : StringUtls.localizedResponse("handler.bookmark.saved", String.valueOf(saved.number()),
                here.get().placeName());
    }
}
