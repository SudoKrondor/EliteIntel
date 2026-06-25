package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.db.managers.ShipLoadoutManager;
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
    public static final String ID = "calculate_neutron_star_route";

    @Override public String llmDescription() { return "Calculate a neutron-boosted economical route to a destination."; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();
    private final ShipLoadoutManager shipLoadoutManager = ShipLoadoutManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        JsonElement key = params.get("efficiency");

        if (key == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.neutronRoute.efficiency"));
        }

        int efficiency = getIntSafely(key.getAsString());
        if (efficiency < 1 || efficiency > 100) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.neutronRoute.efficiency"));
        }

        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        String destination = ClipboardUtils.getClipboardText();

        ShipLoadOutDto shipLoadout = shipLoadoutManager.get();
        if (shipLoadout == null) {
            return null;
        }

        double maxJumpRange = shipLoadout.getMaxJumpRange();
        // Low-range note is folded into the single outcome rather than spoken separately.
        String warning = maxJumpRange < 20 ? StringUtls.localizedLlm("handler.neutronRoute.lowRangeWarning") : "";

        NeutronStarRouteClient client = new NeutronStarRouteClient();
        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria(
                        location.getStarName(), destination, efficiency, maxJumpRange, 0
                )
        );

        if (route != null && route.getResult() != null && route.getResult().getTotalJumps() > 0) {
            neutronStarRouteManager.saveNeutronStarRoute(route);
            String found = StringUtls.localizedLlm("handler.neutronRoute.found", destination, route.getResult().getTotalJumps());
            return CommandOutcome.critical(warning.isEmpty() ? found : warning + " " + found);
        }
        // No route found: surface the low-range warning if there was one, otherwise stay silent.
        return warning.isEmpty() ? null : CommandOutcome.critical(warning);
    }
}
