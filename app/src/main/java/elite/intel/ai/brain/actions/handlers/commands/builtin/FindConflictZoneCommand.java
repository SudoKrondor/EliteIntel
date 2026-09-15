package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.ConflictZoneManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.signals.WarZone;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Plots a route to the best conflict zone system the app knows of, for fighting a faction war for
 * combat bonds.
 * <p>
 * The twin of {@link FindBountyHuntingGroundCommand} for the other kind of fighting. There is no
 * contract to take: the commander flies there, drops into a zone, picks a side and is paid a bond
 * per kill. Ranking prefers the hardest intensity present over the nearest system, because within
 * the range the commander named the question is where the real fighting is.
 * <p>
 * Answered from wars sighted in the last week, by the commander's own FSS or by anyone on the
 * EDDN relay - a war older than that is over.
 */
@RegisterCommand
public final class FindConflictZoneCommand implements IntelCommand {

    public static final String ID = "find_conflict_zone_for_combat_bonds";

    private static final int DEFAULT_RANGE_LY = 100;

    private final ConflictZoneManager conflictZones = ConflictZoneManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Plot a route to a star system with active conflict zones (war zones), to fight for a faction "
                + "and earn combat bonds. Not for bounty hunting or resource extraction sites. Answered from wars "
                + "sighted in the last week, within the range in light years given by 'key'.";
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

        List<WarZone> zones = conflictZones.bestWarZones(here, range);
        if (zones.isEmpty()) return StringUtls.localizedResponse("handler.war.noZonesKnown", range);

        WarZone best = zones.getFirst();
        if (best.starSystem().equalsIgnoreCase(here.primaryStar())) {
            return StringUtls.localizedResponse("handler.war.alreadyInZone", ConflictZoneSpeech.zones(best));
        }

        // Non-terminal announcement: route plotting below must still run, so the finding is voiced via
        // filler rather than returned.
        VegaRuntime.narrator().filler(
                ConflictZoneSpeech.zones(best) + " " + StringUtls.localizedResponse(
                        "handler.pirate.groundDistance", Math.round(best.distanceLy())), false);

        RoutePlotter plotter = new RoutePlotter();
        return plotter.plotRoute(best.starSystem());
    }
}
