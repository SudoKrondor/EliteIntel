package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.LocationBookmarkManager;
import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.gameapi.bookmarks.PendingSurfaceTarget;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.ui.overlay.LocationBookmarkCard;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Optional;

/**
 * "Navigate to bookmark N" - plots a route to the bookmark's star system, the only place the galaxy map can
 * plot to.
 * <p>
 * A bookmarked spot on the ground also gets surface guidance, but not yet: guidance knows no body, so it waits
 * in {@link PendingSurfaceTarget} until the ship approaches the right planet. Already there, it starts at once.
 */
@RegisterCommand
public final class NavigateToLocationBookmarkCommand implements IntelCommand {
    public static final String ID = "navigate_to_location_bookmark";

    @Override
    public String llmDescription() {
        return "Plot a route to the saved location bookmark with number 'key' (the number shown on the bookmark list).";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Route plotting is available anywhere in the game.
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
        Optional<LocationBookmark> found = LocationBookmarkManager.getInstance().byNumber(number);
        if (found.isEmpty()) return StringUtls.localizedResponse("handler.bookmark.notFound", String.valueOf(number));

        LocationBookmark bookmark = found.get();
        LocationBookmarkCard.getInstance().close();
        PendingSurfaceTarget pending = PendingSurfaceTarget.getInstance();
        pending.clear();

        String system = bookmark.starSystem();
        boolean inSystem = system.equalsIgnoreCase(PlayerSession.getInstance().getPrimaryStarName());

        if (bookmark.hasCoordinates()) {
            if (inSystem && onBody(bookmark.planetName())) {
                PendingSurfaceTarget.startGuidance(bookmark);
                return StringUtls.localizedResponse("handler.bookmark.guidanceNow");
            }
            pending.arm(bookmark);
            return inSystem
                    ? StringUtls.localizedResponse("handler.bookmark.hereSurface", bookmark.planetName())
                    : plot(StringUtls.localizedResponse("handler.bookmark.routeToSurface", system, bookmark.planetName()), system);
        }

        return switch (bookmark.kind()) {
            case SYSTEM -> inSystem
                    ? StringUtls.localizedResponse("handler.bookmark.hereSystem", system)
                    : plot(StringUtls.localizedResponse("handler.bookmark.routeToSystem", system), system);
            case PLANETARY_PORT -> inSystem
                    ? StringUtls.localizedResponse("handler.bookmark.herePlanetaryPort", bookmark.spokenName(),
                    bookmark.planetName())
                    : plot(StringUtls.localizedResponse("handler.bookmark.routeToPlanetaryPort", system,
                    bookmark.spokenName(), bookmark.planetName()), system);
            case STATION -> inSystem
                    ? StringUtls.localizedResponse("handler.bookmark.herePort", bookmark.spokenName())
                    : plot(StringUtls.localizedResponse("handler.bookmark.routeToPort", system, bookmark.spokenName()), system);
            case PLANET, SURFACE -> inSystem
                    ? StringUtls.localizedResponse("handler.bookmark.herePlanet", bookmark.planetName())
                    : plot(StringUtls.localizedResponse("handler.bookmark.routeToPlanet", system, bookmark.planetName()), system);
        };
    }

    private static String plot(String answer, String system) {
        return new RoutePlotter().plotRouteAnd(answer, system);
    }

    /**
     * Whether the ship is over or on this body now - the game reports a surface position against it.
     */
    private static boolean onBody(String planetName) {
        Status status = Status.getInstance();
        GameEvents.StatusEvent snapshot = status.getStatus();
        return status.hasLatLong() && planetName != null && planetName.equalsIgnoreCase(snapshot.getBodyName());
    }
}
