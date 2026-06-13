package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.station.interstellarfactors.InterstellarFactorsResultDto;
import elite.intel.search.spansh.station.interstellarfactors.InterstellarFactorsSearch;
import elite.intel.util.StringUtls;

import java.util.List;

public class PlotRouteToInterstellarFactorsHandler implements CommandHandler {

    private final LocationManager locationManager = LocationManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        LocationDao.Coordinates coordinates = locationManager.getGalacticCoordinates();
        if (coordinates == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.interstellarFactors.noCoords")));
            return;
        }
        List<InterstellarFactorsResultDto.Result> results = InterstellarFactorsSearch.findNearestInterstellarFactors(
                coordinates.x(), coordinates.y(), coordinates.z(), 100, 6000
        );

        if (results == null || results.isEmpty()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.interstellarFactors.notFound")));
            return;
        }

        String stationName = results.getFirst().getStationName();
        String starName = results.getFirst().getSystemName();
        RoutePlotter routePlotter = new RoutePlotter();
        routePlotter.plotRoute(starName);

        String announcement = StringUtls.localizedLlm("handler.interstellarFactors.visit", stationName, starName);
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(announcement));
        reminderManager.setReminder("Visit Interstellar Factors at " + stationName, starName);
    }
}
