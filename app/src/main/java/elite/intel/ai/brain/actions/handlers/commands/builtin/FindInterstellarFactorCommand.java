package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.ReminderContact;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.station.CurrentSystemFilter;
import elite.intel.gameapi.search.spansh.station.interstellarfactors.InterstellarFactorsResultDto;
import elite.intel.gameapi.search.spansh.station.interstellarfactors.InterstellarFactorsSearch;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
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

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest Interstellar Factors contact to pay off bounties and fines.";
    }


    private final LocationManager locationManager = LocationManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /// Route plotting available anywhere in the game
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        LocationDao.Coordinates coordinates = locationManager.getGalacticCoordinates();
        if (coordinates == null) {
            return StringUtls.localizedResponse("handler.interstellarFactors.noCoords");
        }
        // WHY: same pad rule the trade route calculation applies - a large ship is only shown ports it can
        // dock at, otherwise the route ends at an outpost it has to turn around and leave, bounty unpaid.
        List<InterstellarFactorsResultDto.Result> results = InterstellarFactorsSearch.findNearestInterstellarFactors(
                coordinates.x(), coordinates.y(), coordinates.z(), 100, 6000, shipManager.requireLargePad()
        );

        results = CurrentSystemFilter.exclude(results, PlayerSession.getInstance().getPrimaryStarName());
        if (results.isEmpty()) {
            return StringUtls.localizedResponse("handler.interstellarFactors.notFound");
        }

        String stationName = results.getFirst().getStationName();
        String starName = results.getFirst().getSystemName();
        String announcement = StringUtls.localizedResponse("handler.interstellarFactors.visit", stationName, starName);
        reminderManager.setReminder(announcement, starName, stationName, ReminderContact.INTERSTELLAR_FACTORS);
        return new RoutePlotter().plotRouteAnd(announcement, starName);
    }
}
