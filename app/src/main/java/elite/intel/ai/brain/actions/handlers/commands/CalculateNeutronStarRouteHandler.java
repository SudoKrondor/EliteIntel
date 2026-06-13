package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import static elite.intel.util.StringUtls.getIntSafely;

public class CalculateNeutronStarRouteHandler implements CommandHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();
    private final ShipLoadoutManager shipLoadoutManager = ShipLoadoutManager.getInstance();


    @Override
    public void handle(String action, JsonObject params, String responseText) {
        JsonElement key = params.get("efficiency");

        if (key == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("Please specify efficiency from 1 to 100"));
            return;
        }

        int efficiency = getIntSafely(key.getAsString());
        if (efficiency < 1 || efficiency > 100) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("Please specify efficiency from 1 to 100"));
            return;
        }

        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        String destination = ClipboardUtils.getClipboardText();
        EventBusManager.publish(
                new MissionCriticalAnnouncementEvent(
                        "Calculating neutron star route from " + location.getStarName()
                                + " to " + destination
                                + " with efficiency " + efficiency
                )
        );
        ShipLoadOutDto shipLoadout = shipLoadoutManager.get();
        if (shipLoadout == null) {
            return;
        }

        double maxJumpRange = shipLoadout.getMaxJumpRange();
        if (maxJumpRange < 20) {
            EventBusManager.publish(
                    new MissionCriticalAnnouncementEvent(
                            """ 
                                            WARNING! We have low jump range. We may end up using a boosted jump
                                            into a system which we do not have enough range to jump out of.
                                            Be careful when plotting using this and check that you can get out of the system
                                            when you are on the galaxy map.
                                    """
                    )
            );
        }


        NeutronStarRouteClient client = new NeutronStarRouteClient();
        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria(
                        location.getStarName(), destination, efficiency, maxJumpRange, 0
                )
        );

        if (route != null && route.getResult() != null && route.getResult().getTotalJumps() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(" Calculated neutron star route to ").append(destination).append(" in ").append(route.getResult().getTotalJumps()).append(" jumps.");
            sb.append(" Ask me to plot the navigate to the next neutron star.");
            neutronStarRouteManager.saveNeutronStarRoute(route);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(sb.toString()));
        }
    }
}
