package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.LocationBookmarkManager;
import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.session.Status;
import elite.intel.ui.overlay.LocationBookmarkCard;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Optional;

/**
 * "Delete bookmark N". A bookmark cannot be got back once deleted, so VEGA names it and asks first.
 * <p>
 * The card closes afterwards: every bookmark after the deleted one has moved up a number, and a list left on
 * screen would be showing numbers that no longer mean what they did a moment ago.
 */
@RegisterCommand
public final class DeleteLocationBookmarkCommand implements IntelCommand {
    public static final String ID = "delete_location_bookmark";

    @Override
    public String llmDescription() {
        return "Delete the saved location bookmark with number 'key' (the number shown on the bookmark list).";
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isDangerous() {
        return true;
    }

    /**
     * Names the bookmark in the question, so the commander confirms the place and not just a number.
     */
    @Override
    public String confirmationPrompt(JsonObject params) {
        Integer number = BookmarkNumber.from(params);
        if (number == null) return null;
        return LocationBookmarkManager.getInstance().byNumber(number)
                .map(bookmark -> StringUtls.localizedResponse("handler.bookmark.confirmDelete",
                        String.valueOf(number), bookmark.placeName()))
                .orElse(null);
    }

    /**
     * App-side bookkeeping (no game input); executable in any location.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return BookmarkNumber.PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Integer number = BookmarkNumber.from(params);
        if (number == null) return StringUtls.localizedResponse("handler.bookmark.invalidNumber");
        Optional<LocationBookmark> deleted = LocationBookmarkManager.getInstance().delete(number);
        if (deleted.isEmpty()) return StringUtls.localizedResponse("handler.bookmark.notFound", String.valueOf(number));
        LocationBookmarkCard.getInstance().close();
        return StringUtls.localizedResponse("handler.bookmark.deleted", String.valueOf(number), deleted.get().placeName());
    }
}
