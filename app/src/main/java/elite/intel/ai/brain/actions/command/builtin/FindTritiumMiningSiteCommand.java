package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.EventBusManager;
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
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (status.isInSrv() || status.isInMainShip()) {
            Number range = GetNumberFromParam.extractRangeParameter(params, 1000);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.carrierFuel.searching", range.intValue())));

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
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.carrierFuel.notFound")));
                return;
            }

            Optional<StellarObjectSearchResultDto.Result> result = tritiumLocations.getResults().stream().findFirst();
            double distance = NavigationUtils.calculateGalacticDistance(result.get().getX(), result.get().getY(), result.get().getZ(), coordinates.x(), coordinates.y(), coordinates.z());
            if(distance > range.intValue()){
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.carrierFuel.notFoundInRange")));
                return;
            }


            String reminder = StringUtls.localizedLlm("handler.carrierFuel.headTo", result.get().getSystemName());
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(reminder));
            ReminderManager reminderManager = ReminderManager.getInstance();
            reminderManager.setReminder(reminder, result.get().getSystemName());
            RoutePlotter routePlotter = new RoutePlotter();
            routePlotter.plotRoute(result.get().getSystemName());

        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.mustBeInShipOrSrv")));
        }
    }
}
