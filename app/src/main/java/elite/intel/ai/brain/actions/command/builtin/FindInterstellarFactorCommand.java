package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.station.interstellarfactors.InterstellarFactorsResultDto;
import elite.intel.search.spansh.station.interstellarfactors.InterstellarFactorsSearch;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Self-describing "find interstellar factor" command.
 * Owns its own execution: body migrated 1:1 from the legacy PlotRouteToInterstellarFactorsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindInterstellarFactorCommand implements IntelCommand {
    public static final String ID = "find_interstellar_factor";

    @Override public String llmDescription() { return "Find the nearest interstellar factor to clear bounties and fines."; }


    private final LocationManager locationManager = LocationManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        LocationDao.Coordinates coordinates = locationManager.getGalacticCoordinates();
        if (coordinates == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.interstellarFactors.noCoords")));
            return;
        }
        List<InterstellarFactorsResultDto.Result> results = InterstellarFactorsSearch.findNearestInterstellarFactors(
                coordinates.x(), coordinates.y(), coordinates.z(), 100, 6000
        );

        if (results == null || results.isEmpty()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.interstellarFactors.notFound")));
            return;
        }

        String stationName = results.getFirst().getStationName();
        String starName = results.getFirst().getSystemName();
        RoutePlotter routePlotter = new RoutePlotter();
        routePlotter.plotRoute(starName);

        String announcement = StringUtls.localizedLlm("handler.interstellarFactors.visit", stationName, starName);
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(announcement));
        reminderManager.setReminder("Visit Interstellar Factors at " + stationName, starName);
    }
}
