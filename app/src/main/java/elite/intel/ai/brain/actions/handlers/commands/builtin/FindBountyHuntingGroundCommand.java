package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.missions.HuntingGround;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Plots a route to the best resource extraction site system the commander knows of, for bounty
 * hunting with no contract involved.
 * <p>
 * The twin of {@link FindPirateMassacreMissionsCommand} for the other half of the activity: no
 * provider, no stacking, no kill count - just somewhere worth dropping into. Ranking prefers the
 * best grade of site present over the nearest ring, because within a range the commander named the
 * question is where the good fighting is.
 */
@RegisterCommand
public final class FindBountyHuntingGroundCommand implements IntelCommand {

    public static final String ID = "find_bounty_hunting_ground";

    private static final int DEFAULT_RANGE_LY = 100;

    private final HuntingGroundManager huntingGrounds = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Plot a route to a star system with resource extraction sites, for bounty hunting without any "
                + "mission. Answered from systems the commander has already flown to, within the range in light "
                + "years given by 'key'.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return SearchRange.PARAMETERS;
    }

    /**
     * Route plotting taps the ship-only GalaxyMapOpen bind, so it works only in the main-ship cockpit.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        int range = SearchRange.lightYears(params, DEFAULT_RANGE_LY);

        Coordinates here = locationManager.getGalacticCoordinates();
        if (here == null) return StringUtls.localizedResponse("handler.pirate.positionUnknown");

        List<HuntingGround> grounds = huntingGrounds.bestHuntingGrounds(here, range);
        if (grounds.isEmpty()) return StringUtls.localizedResponse("handler.pirate.noGroundsKnown", range);

        HuntingGround best = grounds.getFirst();
        if (best.starSystem().equalsIgnoreCase(here.primaryStar())) {
            return StringUtls.localizedResponse("handler.pirate.alreadyInGround",
                    HuntingGroundSpeech.sitesOrUnknown(best.starSystem(), best.sites()));
        }

        // Non-terminal announcement: route plotting below must still run, so the finding is voiced via
        // filler rather than returned.
        VegaRuntime.narrator().filler(
                HuntingGroundSpeech.sitesOrUnknown(best.starSystem(), best.sites())
                        + " " + StringUtls.localizedResponse("handler.pirate.groundDistance",
                        Math.round(best.distanceLy())), false);

        RoutePlotter plotter = new RoutePlotter();
        return plotter.plotRoute(best.starSystem());
    }
}
