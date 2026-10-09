package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.LocationBookmarkManager;
import elite.intel.session.Status;
import elite.intel.ui.overlay.LocationBookmarkCard;
import elite.intel.ui.overlay.NativeHudOverlay;
import elite.intel.util.StringUtls;

/**
 * "Show bookmarks" - puts the bookmark list on the HUD overlay, over whatever card was there.
 * <p>
 * WHY the list is never read aloud when the overlay is off: system names are catalogue strings, and a list of
 * them spoken one after another is noise the commander cannot pick a number out of.
 */
@RegisterCommand
public final class ShowLocationBookmarksCommand implements IntelCommand {
    public static final String ID = "show_location_bookmarks";

    @Override
    public String llmDescription() {
        return "Show the commander's saved location bookmarks on the HUD overlay, numbered newest first.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * App-side display (no game input); available anywhere.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        if (!NativeHudOverlay.isShowing()) return StringUtls.localizedResponse("handler.bookmark.overlayOff");
        if (LocationBookmarkManager.getInstance().count() == 0)
            return StringUtls.localizedResponse("handler.bookmark.none");
        LocationBookmarkCard.getInstance().open();
        return StringUtls.localizedResponse("handler.bookmark.showing");
    }
}
