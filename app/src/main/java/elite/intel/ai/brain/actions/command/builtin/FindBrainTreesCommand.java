package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.BrainTreeManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.util.StringUtls;

import static elite.intel.util.StringUtls.capitalizeWords;

/**
 * Self-describing "find brain trees" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindBrainTreesHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindBrainTreesCommand implements IntelCommand {
    public static final String ID = "find_brain_trees";


    private final BrainTreeManager brainTreeManager = BrainTreeManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (brainTreeManager.getCount() == 0) {
            brainTreeManager.retrieveFromSpansh();
        }

        JsonElement key = params.get("key");
        if (key == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.brainTrees.didNotCatch")));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.brainTrees.notFound")));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.brainTrees.found", result.getSystemName(), result.getDistance(), result.getBodyName())));
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(result.getSystemName());
            ReminderManager.getInstance().setReminder(
                    StringUtls.localizedLlm("handler.brainTrees.reminder", result.getSystemName(), result.getBodyName()),
                    result.getSystemName()
            );
        }
    }
}
