package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.BrainTreeManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearchResultDto;
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

    @Override public String llmDescription() { return "Find nearby Brain Tree biological sites."; }


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
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (brainTreeManager.getCount() == 0) {
            brainTreeManager.retrieveFromSpansh();
        }

        JsonElement key = params.get(PARAM_KEY);
        if (key == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.brainTrees.didNotCatch")));
            return;
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
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.brainTrees.notFound")));
        } else {
            double distance = calculateDistance(coordinates, result.getX(), result.getY(), result.getZ());
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.brainTrees.found", result.getSystemName(), distance, result.getBodyName())));
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(result.getSystemName());
            ReminderManager.getInstance().setReminder(
                    StringUtls.localizedLlm("handler.brainTrees.reminder", result.getSystemName(), result.getBodyName()),
                    result.getSystemName()
            );
        }
    }

    private double calculateDistance(LocationDao.Coordinates coordinates, double x, double y, double z) {
        return NavigationUtils.calculateGalacticDistance(
                coordinates.x(), coordinates.y(), coordinates.z(),
                x, y, z

        );
    }
}
