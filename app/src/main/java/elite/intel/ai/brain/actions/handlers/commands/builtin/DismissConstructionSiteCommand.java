package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.gameapi.colonisation.ActiveConstructionSite;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * "Dismiss the construction site" - stops the current build being volunteered.
 * <p>
 * Hauling to a colony has no end event in the journal. A commander does a few runs and then goes trading,
 * and nothing ever says they have finished, so {@link ActiveConstructionSite} eventually withdraws the card
 * on staleness alone. This is the way to say it today rather than in three days.
 * <p>
 * <b>Nothing is deleted.</b> This clears which build is CURRENT, and that is all. Every manifest the
 * commander has collected stays in the database - a squadron can be colonising several systems at once, and
 * throwing away the records of the others because they said they were done with this one would be a much
 * bigger answer than the question. Landing at any depot makes that one current again.
 * <p>
 * Nor is an older build promoted in its place: the HUD goes quiet, which is what was asked for.
 */
@RegisterCommand
public final class DismissConstructionSiteCommand implements IntelCommand {
    public static final String ID = "dismiss_construction_site";

    private final ConstructionSiteManager constructionSiteManager = ConstructionSiteManager.getInstance();

    @Override
    public String llmDescription() {
        return "Stop tracking the colonisation construction site the commander was last docked at: remove it "
                + "from the HUD and stop offering to shop or navigate for it. Takes no arguments. Use when "
                + "the commander says they are done with the construction site or wants it cleared.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * App-side bookkeeping (no game input), and offered whenever there is a current build - including a stale
     * one the card has already withdrawn, which is exactly the one a commander is most likely to want gone.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return constructionSiteManager.currentSite() != null;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Site site = constructionSiteManager.currentSite();
        if (site == null) {
            return StringUtls.localizedResponse("query.construction.noSite");
        }
        String name = site.getStationName();
        // Nothing is promoted in its place. An older build the commander happened to visit before this one
        // is not what they asked to see, and volunteering it would read as the card refusing to go away.
        constructionSiteManager.dismissCurrent();
        return StringUtls.localizedResponse("handler.construction.dismissed", describe(name));
    }

    private static String describe(String stationName) {
        return stationName == null || stationName.isBlank()
                ? StringUtls.localizedResponse("handler.construction.thisSite")
                : stationName;
    }
}
