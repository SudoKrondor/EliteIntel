package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.BrainTreeManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.session.Status;
import elite.intel.util.NavigationUtils;
import elite.intel.util.StringUtls;

import java.util.List;

import static elite.intel.util.StringUtls.capitalizeWords;

/**
 * Self-describing "find brain trees" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindBrainTreesHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindBrainTreesCommand implements IntelCommand {
    public static final String ID = "find_brain_trees";

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest Brain Trees biological site that yields the raw material named in 'key'.";
    }


    private final BrainTreeManager brainTreeManager = BrainTreeManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    private static final String PARAM_KEY = "key";

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
        if (brainTreeManager.getCount() == 0) {
            brainTreeManager.retrieveFromSpansh();
        }

        JsonElement key = params.get(PARAM_KEY);
        if (key == null) {
            return StringUtls.localizedResponse("handler.brainTrees.didNotCatch");
        }

        String material =
                capitalizeWords(
                        FuzzySearch.fuzzyMaterialNameSearch(
                                key.getAsString(), 8
                        )
                );

        LocationDao.Coordinates coordinates = locationManager.getGalacticCoordinates();
        StellarObjectSearchResultDto.Result result = brainTreeManager.findNearestWithMaterial(material, coordinates.x(), coordinates.y(), coordinates.z());
        if (result == null) {
            return StringUtls.localizedResponse("handler.brainTrees.notFound");
        } else {
            double distance = calculateDistance(coordinates, result.getX(), result.getY(), result.getZ());
            CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.brainTrees.found", result.getSystemName(), distance, result.getBodyName()), false);
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(result.getSystemName());
            ReminderManager.getInstance().setReminder(
                    StringUtls.localizedResponse("handler.brainTrees.reminder", result.getSystemName(), result.getBodyName()),
                    result.getSystemName()
            );
        }
        return null;
    }

    private double calculateDistance(LocationDao.Coordinates coordinates, double x, double y, double z) {
        return NavigationUtils.calculateGalacticDistance(
                coordinates.x(), coordinates.y(), coordinates.z(),
                x, y, z

        );
    }
}
