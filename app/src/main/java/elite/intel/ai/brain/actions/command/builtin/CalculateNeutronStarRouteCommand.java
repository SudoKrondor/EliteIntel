package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.search.spansh.neutronroute.NeutronStarRoute;
import elite.intel.search.spansh.neutronroute.NeutronStarRouteCalculatorCriteria;
import elite.intel.search.spansh.neutronroute.NeutronStarRouteClient;
import elite.intel.session.PlayerSession;
import elite.intel.util.ClipboardUtils;
import elite.intel.util.StringUtls;

import static elite.intel.util.StringUtls.getIntSafely;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CalculateNeutronStarRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CalculateNeutronStarRouteCommand implements IntelCommand {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();
    private final ShipLoadoutManager shipLoadoutManager = ShipLoadoutManager.getInstance();

    @Override
    public String id() {
        return CommandIds.CALCULATE_NEUTRON_STAR_ROUTE;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        JsonElement key = params.get("efficiency");

        if (key == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.efficiency")));
            return;
        }

        int efficiency = getIntSafely(key.getAsString());
        if (efficiency < 1 || efficiency > 100) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.efficiency")));
            return;
        }

        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        String destination = ClipboardUtils.getClipboardText();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.calculating", location.getStarName(), destination, efficiency)));

        ShipLoadOutDto shipLoadout = shipLoadoutManager.get();
        if (shipLoadout == null) {
            return;
        }

        double maxJumpRange = shipLoadout.getMaxJumpRange();
        if (maxJumpRange < 20) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.lowRangeWarning")));
        }


        NeutronStarRouteClient client = new NeutronStarRouteClient();
        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria(
                        location.getStarName(), destination, efficiency, maxJumpRange, 0
                )
        );

        if (route != null && route.getResult() != null && route.getResult().getTotalJumps() > 0) {
            neutronStarRouteManager.saveNeutronStarRoute(route);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.neutronRoute.found", destination, route.getResult().getTotalJumps())));
        }
    }
}
