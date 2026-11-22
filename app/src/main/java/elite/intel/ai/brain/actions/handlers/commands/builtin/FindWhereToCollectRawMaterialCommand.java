package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.MaterialManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.gameapi.search.PermitLockedSystems;
import elite.intel.gameapi.search.spansh.rawmaterials.MaterialBodyClient;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto.Result;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;

/**
 * Where to collect a raw material in the wild, and a route plotted there: a brain tree site when one is within
 * range, since it yields the most at every grade, else the planet surface best for it. The commander need not
 * know that brain trees exist to be sent to one. The choice itself is {@link RawMaterialSource}.
 */
@RegisterCommand
public final class FindWhereToCollectRawMaterialCommand implements IntelCommand {
    public static final String ID = "find_where_to_collect_raw_material";

    private static final String PARAM_KEY = "key";
    private static final String PARAM_MAX_DISTANCE = "max_distance";

    /**
     * How far a brain tree may be before a surface nearby is preferred, when the commander names no range.
     * Around the Bubble brain trees are plentiful (6877 bodies within 1000 ly of Sol). A commander who will not
     * fly that far for a common material says a smaller range.
     */
    static final int DEFAULT_RANGE_LY = 500;

    /** Material types the catalogue files; only raw ones are collected from a planet. */
    private static final String RAW = "Raw";
    private static final String MANUFACTURED = "Manufactured";

    /**
     * Brain trees asked of Spansh: enough that a permit-locked one, or two, at the head of the list still
     * leaves an answer.
     */
    private static final int BRAIN_TREE_CANDIDATES = 5;

    /** Surfaces asked of Spansh: the nearest ones, from which the richest within two jumps is chosen. */
    private static final int SURFACE_CANDIDATES = 50;

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private final LocationManager locationManager = LocationManager.getInstance();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The raw material (element) to collect, e.g. tellurium, polonium, iron.",
                List.of("tellurium", "polonium"),
                "Extract the material name verbatim in lower case; do not translate.");
        key.validate();
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum galactic search radius in light years (ly). If omitted, a default range is used.",
                List.of("100", "500"),
                "Extract the distance limit in light years if the commander states one, as digits.");
        maxDistance.validate();
        return List.of(key, maxDistance);
    }

    @Override
    public String llmDescription() {
        return "Find and plot a route to the best place to collect the raw material (element) named in 'key', "
                + "e.g. tellurium: a Brain Trees site, else the planet surface richest in it, within 'max_distance' ly. "
                + "Not ring mining (find_mining_site), not trading materials (find_raw_material_trader).";
    }

    @Override
    public String id() {
        return ID;
    }

    /** Plotting routes is available anywhere in the game. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement key = params.get(PARAM_KEY);
        String symbol = key == null ? null : FuzzySearch.fuzzyMaterialSymbol(key.getAsString(), 8);
        MaterialNameDao.Material material = symbol == null ? null : MaterialManager.getInstance().find(symbol);
        if (material == null) {
            return StringUtls.localizedResponse("handler.rawMaterial.didNotCatch");
        }
        String spoken = FuzzySearch.localizedMaterialName(material.getSymbol());
        if (!RAW.equalsIgnoreCase(material.getMaterialType())) {
            // A brain tree and a planet surface yield only what the planet is made of.
            return StringUtls.localizedResponse(MANUFACTURED.equalsIgnoreCase(material.getMaterialType())
                    ? "handler.rawMaterial.notRaw.manufactured"
                    : "handler.rawMaterial.notRaw.encoded", spoken);
        }
        // Spansh lists a body's materials by their English names.
        String english = material.getName();
        int range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE_LY).intValue();
        boolean brainTreesOnly = BrainTreeMention.in(responseText);

        // Two Spansh searches may follow, each taking seconds.
        VegaRuntime.narrator().filler(
                StringUtls.localizedResponse("handler.rawMaterial.searching", spoken, range), false);

        LocationDao.Coordinates from = locationManager.getGalacticCoordinates();
        MaterialBodyClient spansh = MaterialBodyClient.getInstance();
        List<Result> brainTrees = reachable(
                spansh.nearestBrainTrees(english, from.x(), from.y(), from.z(), BRAIN_TREE_CANDIDATES));
        RawMaterialSource.Pick pick = RawMaterialSource.pick(english, brainTrees,
                () -> reachable(spansh.nearestLandableBodies(english, from.x(), from.y(), from.z(), range, SURFACE_CANDIDATES)),
                range, twoJumpsLy(), brainTreesOnly);

        return switch (pick) {
            case RawMaterialSource.BrainTree(Result site) -> plot(site,
                    StringUtls.localizedResponse("handler.rawMaterial.brainTree.found",
                            site.getSystemName(), Math.round(site.getDistance()), site.getBodyName(), spoken),
                    StringUtls.localizedResponse("handler.rawMaterial.brainTree.reminder", site.getBodyName(), spoken));
            case RawMaterialSource.Surface(Result site, double share) -> plot(site,
                    StringUtls.localizedResponse("handler.rawMaterial.surface.found",
                            site.getSystemName(), Math.round(site.getDistance()), site.getBodyName(), spoken,
                            range, Math.round(share * 10) / 10.0),
                    StringUtls.localizedResponse("handler.rawMaterial.surface.reminder", site.getBodyName(), spoken));
            case RawMaterialSource.BrainTreeBeyondRange(Result site) ->
                    StringUtls.localizedResponse("handler.rawMaterial.brainTree.beyondRange",
                            spoken, range, site.getSystemName(), site.getBodyName(), Math.round(site.getDistance()));
            case RawMaterialSource.NoBrainTree() ->
                    StringUtls.localizedResponse("handler.rawMaterial.brainTree.none", spoken);
            case RawMaterialSource.Nothing() ->
                    StringUtls.localizedResponse("handler.rawMaterial.notFound", spoken, range);
        };
    }

    /** Speaks the find, leaves the arrival errand, and plots the route. */
    private static String plot(Result site, String found, String reminder) {
        VegaRuntime.narrator().filler(found, false);
        ReminderManager.getInstance().setReminder(reminder, site.getSystemName());
        // The find was spoken above, so the plotter's note is all that is left to say, and it says something
        // only when no new route was plotted.
        return new RoutePlotter().plotRouteAnd(null, site.getSystemName());
    }

    private static List<Result> reachable(List<Result> sites) {
        return PermitLockedSystems.reachable(sites, Result::getSystemName);
    }

    /**
     * Twice the ship's jump range, or 0 before the game has reported a loadout: two jumps are no hassle, so
     * within them the richest surface beats the nearest.
     */
    private static double twoJumpsLy() {
        ShipLoadOutDto loadout = PlayerSession.getInstance().getShipLoadout();
        return loadout == null ? 0 : loadout.getMaxJumpRange() * 2;
    }
}
