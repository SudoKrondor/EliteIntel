package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.station.interstellarfactors.InterstellarFactorsResultDto;
import elite.intel.search.spansh.station.interstellarfactors.InterstellarFactorsSearch;

import java.util.List;

public class PlotRouteToInterstellarFactorsHandler implements CommandHandler {

    private final LocationManager locationManager = LocationManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        LocationDao.Coordinates coordinates = locationManager.getGalacticCoordinates();
        if (coordinates == null) {
            EventBusManager.publish(
                    new MissionCriticalAnnouncementEvent(
                            "Unable to comply. Your coordinates are not available. Make at least one hyperspace jump so I can get your coordinates"
                    )
            );
            return;
        }
        List<InterstellarFactorsResultDto.Result> results = InterstellarFactorsSearch.findNearestInterstellarFactors(
                coordinates.x(), coordinates.y(), coordinates.z(), 100, 6000
        );

        if (results == null || results.isEmpty()) {
            EventBusManager.publish(
                    new MissionCriticalAnnouncementEvent(
                            "Unable to find any Interstellar Factor locations in Spansh at this time"
                    )
            );
            return;
        }

        String stationName = results.getFirst().getStationName();
        String starName = results.getFirst().getSystemName();
        RoutePlotter routePlotter = new RoutePlotter();
        routePlotter.plotRoute(starName);

        EventBusManager.publish(
                new MissionCriticalAnnouncementEvent("Visit " + stationName + " port in " + starName + " system!"
                )
        );
        reminderManager.setReminder("Visit Interstellar Factors at " + stationName, starName);
    }
}
