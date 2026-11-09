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
import elite.intel.gameapi.search.PermitLockedSystems;
import elite.intel.gameapi.search.spansh.braintrees.BrainTreeClient;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.session.Status;
import elite.intel.util.NavigationUtils;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Self-describing "find brain trees" command: the nearest brain tree site that yields the raw material
 * the commander named, asked of Spansh live, then a route plotted to it.
 */
@RegisterCommand
public final class FindBrainTreesCommand implements IntelCommand {
    public static final String ID = "find_brain_trees";

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest Brain Trees biological site that yields the raw material named in 'key'.";
    }


    private final LocationManager locationManager = LocationManager.getInstance();

    private static final String PARAM_KEY = "key";

    /**
     * The material type a planet's surface holds, and so the only kind a brain tree yields.
     */
    private static final String RAW = "Raw";

    /**
     * How many of the nearest sites to ask Spansh for: enough that a permit-locked one, or two, at the head
     * of the list still leaves an answer.
     */
    private static final int NEAREST_CANDIDATES = 5;

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The biological material / genus to look for at a Brain Tree site, e.g. tellurium, ruthenium.",
                List.of("tellurium", "ruthenium"),
                "Extract the material name verbatim in lower case; do not translate.");
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return ID;
    }


    @Override
    ///Plotting routes are available anywhere in the game
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
        if (key == null) {
            return StringUtls.localizedResponse("handler.brainTrees.didNotCatch");
        }

        String material = rawMaterialName(key.getAsString());
        if (material == null) {
            // Not a raw material we know: a brain tree yields only what its planet is made of, so a
            // manufactured or encoded material, or a word that names none, has no site to find.
            return StringUtls.localizedResponse("handler.brainTrees.notFound");
        }

        LocationDao.Coordinates coordinates = locationManager.getGalacticCoordinates();
        // The nearest site the commander may actually fly to: Spansh is asked for a few in case the
        // closest sits behind a permit.
        List<StellarObjectSearchResultDto.Result> nearest = PermitLockedSystems.reachable(
                BrainTreeClient.getInstance().nearestWithMaterial(
                        material, coordinates.x(), coordinates.y(), coordinates.z(), NEAREST_CANDIDATES),
                StellarObjectSearchResultDto.Result::getSystemName);
        if (nearest.isEmpty()) {
            return StringUtls.localizedResponse("handler.brainTrees.notFound");
        }
        StellarObjectSearchResultDto.Result result = nearest.getFirst();
        double distance = calculateDistance(coordinates, result.getX(), result.getY(), result.getZ());
        VegaRuntime.narrator().filler(StringUtls.localizedResponse("handler.brainTrees.found", result.getSystemName(), distance, result.getBodyName()), false);
        ReminderManager.getInstance().setReminder(
                StringUtls.localizedResponse("handler.brainTrees.reminder", result.getBodyName()),
                result.getSystemName()
        );
        // The find itself was spoken as filler above, so the plotter's note is the only thing left to say -
        // and it says something only when no new route was plotted.
        return new RoutePlotter().plotRouteAnd(null, result.getSystemName());
    }

    /**
     * The English name of the raw material the commander named, as Spansh spells it, or null when the
     * words name no raw material. Matched in the commander's language, sent in English.
     */
    private static String rawMaterialName(String spoken) {
        String symbol = FuzzySearch.fuzzyMaterialSymbol(spoken, 8);
        if (symbol == null) return null;
        MaterialNameDao.Material material = MaterialManager.getInstance().find(symbol);
        if (material == null || !RAW.equalsIgnoreCase(material.getMaterialType())) return null;
        return material.getName();
    }

    private double calculateDistance(LocationDao.Coordinates coordinates, double x, double y, double z) {
        return NavigationUtils.calculateGalacticDistance(
                coordinates.x(), coordinates.y(), coordinates.z(),
                x, y, z

        );
    }
}
