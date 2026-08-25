package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.colonisation.ActiveConstructionSite;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * "Take me back to the construction site" - the return leg of a hauling run.
 * <p>
 * <b>Which site.</b> The one the commander last stood on. A squadron can be colonising several systems at
 * once, and a single system can hold more than one depot, so "the construction site" is never a lookup by
 * place - it is the build they were last at, which is by definition the one they left to go shopping.
 * <p>
 * Offered whenever that build is still live (see {@link ActiveConstructionSite}) and deliberately NOT gated
 * on cargo: flying back empty to read the manifest off the panel, or to see what somebody else delivered
 * while you were away, is a perfectly ordinary reason to want the route.
 * <p>
 * A route can only ever be plotted to a SYSTEM, and a system can hold several depots - so the spoken answer
 * names the port as well, and the reminder carries it to the far end where it is actually needed.
 */
@RegisterCommand
public final class NavigateToConstructionSiteCommand implements IntelCommand {
    public static final String ID = "navigate_to_construction_site";

    private final ConstructionSiteManager constructionSiteManager = ConstructionSiteManager.getInstance();

    @Override
    public String llmDescription() {
        return "Plot a route back to the colonisation construction site the commander is hauling to - the "
                + "one they were last docked at. Takes no arguments. Use when the commander wants to return "
                + "to the construction site, the build, or the colony depot.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Only while there is a live build to go back to, and only once we know which system it is in - the
     * depot event carries a MarketID and nothing else, so a site first seen after a restart may have no
     * name or system until the next docking writes one.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        Site site = constructionSiteManager.currentSite();
        return ActiveConstructionSite.isLive(site) && site.getStarSystem() != null && !site.getStarSystem().isBlank();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Site site = constructionSiteManager.currentSite();
        if (!ActiveConstructionSite.isLive(site)) {
            return StringUtls.localizedResponse("handler.construction.noSiteToReturnTo");
        }
        String system = site.getStarSystem();
        if (system == null || system.isBlank()) {
            return StringUtls.localizedResponse("handler.construction.siteSystemUnknown");
        }

        // Hauling to a build is mostly done WITHIN one system - the depot, the commander's carrier and the
        // nearby markets all in the same place - so "take me back" is asked far more often from inside that
        // system than from outside it. There is no route to plot to the system you are standing in: the
        // galaxy map would be opened only to type the name of the system it is already centred on. The
        // answer is which pad to head for, which matters all the more because a system can hold several.
        String currentSystem = PlayerSession.getInstance().getPrimaryStarName();
        if (currentSystem != null && currentSystem.equalsIgnoreCase(system)) {
            return StringUtls.localizedResponse("handler.construction.siteInThisSystem",
                    siteName(site), (int) Math.round(site.getProgress() * 100));
        }

        String answer = StringUtls.localizedResponse("handler.construction.returning",
                siteName(site), system, (int) Math.round(site.getProgress() * 100));
        // The port, not just the system: the route ends at the system and the commander still has to pick
        // the right pad out of however many that system holds.
        ReminderManager.getInstance().setReminder(answer, system, site.getStationName(), null);
        return new RoutePlotter().plotRouteAnd(answer, system);
    }

    private static String siteName(Site site) {
        String name = site.getStationName();
        return name == null || name.isBlank()
                ? StringUtls.localizedResponse("handler.construction.thisSite")
                : name;
    }
}
