package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.companion.CompanionRuntime;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.stellarobjects.ReserveLevel;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearch;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.session.Status;
import elite.intel.util.NavigationUtils;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.Optional;

/**
 * Self-describing "find tritium mining site" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindCarrierFuelMiningSiteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindTritiumMiningSiteCommand implements IntelCommand {
    public static final String ID = "find_tritium_mining_site";

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest pristine Tritium mining site (Tritium is fleet-carrier fuel).";
    }


    @Override
    public String id() {
        return ID;
    }

    /// Maps and Routes are available anywhere in the game
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Number range = GetNumberFromParam.extractRangeParameter(params, 1000);
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.carrierFuel.searching", range.intValue()), false);

        ShipRouteManager shipRouteManager = ShipRouteManager.getInstance();
        shipRouteManager.clearRoute();
        LocationDao.Coordinates coordinates = LocationManager.getInstance().getGalacticCoordinates();
        StellarObjectSearchResultDto tritiumLocations = StellarObjectSearch.getInstance()
                .findRings(
                        "Tritium",
                        ReserveLevel.PRISTINE,
                        coordinates,
                        range.intValue()
                );

        if (tritiumLocations == null || tritiumLocations.getResults().isEmpty()) {
            return StringUtls.localizedLlm("handler.carrierFuel.notFound");
        }

        Optional<StellarObjectSearchResultDto.Result> result = tritiumLocations.getResults().stream().findFirst();
        double distance = NavigationUtils.calculateGalacticDistance(result.get().getX(), result.get().getY(), result.get().getZ(), coordinates.x(), coordinates.y(), coordinates.z());
        if (distance > range.intValue()) {
            return StringUtls.localizedLlm("handler.carrierFuel.notFoundInRange");
        }

        String reminder = StringUtls.localizedLlm("handler.carrierFuel.headTo", result.get().getSystemName());
        CompanionRuntime.narrator().filler(reminder, false);
        ReminderManager reminderManager = ReminderManager.getInstance();
        reminderManager.setReminder(reminder, result.get().getSystemName());
        RoutePlotter routePlotter = new RoutePlotter();
        return routePlotter.plotRoute(result.get().getSystemName());
    }
}
