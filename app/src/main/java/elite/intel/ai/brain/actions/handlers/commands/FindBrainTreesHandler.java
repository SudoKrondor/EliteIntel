package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.BrainTreeManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import static elite.intel.util.StringUtls.capitalizeWords;

public class FindBrainTreesHandler implements CommandHandler {

    private final BrainTreeManager brainTreeManager = BrainTreeManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();


    @Override public void handle(String action, JsonObject params, String responseText) {
        if (brainTreeManager.getCount() == 0) {
            //NOTE:
            // We can get local data from Spansh, this is a fast all.
            // Brain Tree's do not disappear form the galaxy, but new locations could be added.
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

    record DataDto(String starSystem, String planetName) implements ToJsonConvertible {
        @Override public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }
}
