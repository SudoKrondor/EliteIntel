package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.companion.CompanionRuntime;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.search.spansh.neutronroute.NeutronStarRoute;
import elite.intel.search.spansh.neutronroute.NeutronStarRouteCalculatorCriteria;
import elite.intel.search.spansh.neutronroute.NeutronStarRouteClient;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.ClipboardUtils;
import elite.intel.util.StringUtls;

import java.util.List;

import static elite.intel.util.StringUtls.getIntSafely;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CalculateNeutronStarRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CalculateNeutronStarRouteCommand implements IntelCommand {
    public static final String ID = "calculate_neutron_star_route";

    @Override
    public String llmDescription() {
        return "Plot a neutron-star-boosted economical route from the current system to the destination on the clipboard; 'efficiency' (1-100) is the Spansh route efficiency percentage.";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();
    private final ShipLoadoutManager shipLoadoutManager = ShipLoadoutManager.getInstance();

    private static final String PARAM_EFFICIENCY = "efficiency";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec efficiency = new ActionParameterSpec(
                PARAM_EFFICIENCY, "number", true,
                "Route efficiency percentage from 1 to 100: lower trades extra jumps for shorter total distance.",
                List.of("60", "100"),
                "Extract the efficiency percentage the commander states (1-100).");
        efficiency.validate();
        return List.of(efficiency);
    }

    @Override
    public String id() {
        return ID;
    }

    /** App-side route calculation (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement key = params.get(PARAM_EFFICIENCY);

        if (key == null) {
            return StringUtls.localizedLlm("handler.neutronRoute.efficiency");
        }

        Integer efficiency = getIntSafely(key.getAsString());
        if (efficiency != null && efficiency < 1 || efficiency > 100) {
            efficiency = 50; // Default if none provided.
        }

        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        String destination = ClipboardUtils.getClipboardText();
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.neutronRoute.calculating", location.getStarName(), destination, efficiency), false);

        ShipLoadOutDto shipLoadout = shipLoadoutManager.get();
        if (shipLoadout == null) {
            return null;
        }

        double maxJumpRange = shipLoadout.getMaxJumpRange();
        // Non-terminal warning: the route calculation below must still run, so voice the line via
        // CompanionRuntime.narrator().filler (spoken, not remembered) instead of returning here.
        if (maxJumpRange < 20) {
            CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.neutronRoute.lowRangeWarning"), false);
        }


        NeutronStarRouteClient client = new NeutronStarRouteClient();
        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria(
                        location.getStarName(), destination, efficiency, maxJumpRange, 0
                )
        );

        if (route != null && route.getResult() != null && route.getResult().getTotalJumps() > 0) {
            neutronStarRouteManager.saveNeutronStarRoute(route);
            return StringUtls.localizedLlm("handler.neutronRoute.found", destination, route.getResult().getTotalJumps());
        }
        return null;
    }
}
