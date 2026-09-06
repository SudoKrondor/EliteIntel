package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.gameapi.search.spansh.neutronroute.NeutronStarRoute;
import elite.intel.gameapi.search.spansh.neutronroute.NeutronStarRouteCalculatorCriteria;
import elite.intel.gameapi.search.spansh.neutronroute.NeutronStarRouteClient;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.ClipboardUtils;
import elite.intel.util.StringUtls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static elite.intel.util.StringUtls.getIntSafely;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CalculateNeutronStarRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CalculateNeutronStarRouteCommand implements IntelCommand {
    public static final String ID = "calculate_neutron_star_route";

    private static final Logger log = LogManager.getLogger(CalculateNeutronStarRouteCommand.class);

    @Override
    public String llmDescription() {
        return "Plot a neutron-star-boosted economical route from the current system to the destination the commander copied from the galaxy map; 'efficiency' (1-100) is the Spansh route efficiency percentage, and 'supercharge' is true when the commander asks for supercharge or overcharge and false when they do not mention it.";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();
    private final ShipLoadoutManager shipLoadoutManager = ShipLoadoutManager.getInstance();

    private static final String PARAM_EFFICIENCY = "efficiency";
    private static final String PARAM_SUPERCHARGE = "supercharge";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    /**
     * Applied whenever the commander does not state an efficiency, so a bare request still plots a route.
     */
    private static final int DEFAULT_EFFICIENCY = 50;

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec efficiency = new ActionParameterSpec(
                PARAM_EFFICIENCY, "number", false,
                "Route efficiency percentage from 1 to 100: lower trades extra jumps for shorter total distance. "
                        + "Optional - omit it when the commander does not state one and " + DEFAULT_EFFICIENCY
                        + " is used.",
                List.of("60", "100"),
                "Extract the efficiency percentage only if the commander states one; never ask for it.");
        efficiency.validate();
        ActionParameterSpec supercharge = new ActionParameterSpec(
                PARAM_SUPERCHARGE, "boolean", false,
                "Whether to plot the route through supercharged jumps: true to use them, false not to. "
                        + "Optional - omit it when the commander does not raise the subject, and the route "
                        + "is plotted without them.",
                List.of("true", "false"),
                "The commander names this or leaves it out; they never give it a number. Supercharge,"
                        + " overcharge and multiplier all mean this parameter, and asking for any of them is"
                        + " true ('with supercharge', 'use the overcharge'). False is for the explicit refusal"
                        + " ('without supercharge') and for not mentioning it at all. Never ask for it.");
        supercharge.validate();
        return List.of(efficiency, supercharge);
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
        // A bare "plot a neutron route" is a complete order: plot it at the defaults rather than asking back.
        Integer efficiency = readNumber(params, PARAM_EFFICIENCY, 1, 100);
        if (efficiency == null) {
            efficiency = DEFAULT_EFFICIENCY;
        }
        // A yes or no, not a number: Spansh offers the supercharge as a tick box - see NeutronStarRouteClient,
        // which owns the two numbers it goes out as. Unstated is a no, which is the planner's own default.
        boolean supercharge = readFlag(params, PARAM_SUPERCHARGE);

        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        String origin = location.getStarName();

        // The destination is whatever the commander copied out of the galaxy map - there is no other way for
        // them to name a system we have never been to, and a misheard system name is not one Spansh can plot
        // from. An empty clipboard is the one failure we can tell them how to fix, so it is named rather than
        // reported as "no route found" after a minute of waiting.
        String destination = ClipboardUtils.getClipboardText();
        destination = destination == null ? "" : destination.trim();
        if (destination.isEmpty()) {
            return StringUtls.localizedResponse("handler.neutronRoute.noDestination");
        }
        // Only reachable before the first Location event of a session, when we do not yet know where we are.
        // Spansh would answer a from-nowhere request with no route at all, so it is short-circuited here.
        if (origin == null || origin.isBlank()) {
            log.warn("Neutron route requested before the current system is known - nothing to plot from");
            return StringUtls.localizedResponse("handler.neutronRoute.notFound");
        }

        CompanionRuntime.narrator().filler(calculatingLine(origin, destination, efficiency, supercharge), false);

        ShipLoadOutDto shipLoadout = shipLoadoutManager.get();
        if (shipLoadout == null) {
            return null;
        }

        double maxJumpRange = shipLoadout.getMaxJumpRange();
        // Non-terminal warning: the route calculation below must still run, so voice the line via
        // CompanionRuntime.narrator().filler (spoken, not remembered) instead of returning here.
        if (maxJumpRange < 20) {
            CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.neutronRoute.lowRangeWarning"), false);
        }


        // WHY: the Spansh plot below can run for minutes - far past the 60s thought watchdog, which by the time it
        // returns has already force-interrupted the commander turn that launched us. A returned outcome would settle
        // on that interrupted thought and be discarded as a late result (route saved, never voiced), so both the
        // success and failure outcomes are announced on the EVENT lane instead - deliberately voiced AND remembered
        // as an event, since a completed plot attempt is a real outcome worth recall.
        NeutronStarRouteClient client = new NeutronStarRouteClient();
        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria(
                        origin, destination, efficiency, maxJumpRange, supercharge
                )
        );

        String outcome;
        if (route != null && route.getResult() != null && route.getResult().getTotalJumps() > 0) {
            neutronStarRouteManager.saveNeutronStarRoute(route);
            outcome = StringUtls.localizedResponse("handler.neutronRoute.found", destination, route.getResult().getTotalJumps());
        } else {
            outcome = StringUtls.localizedResponse("handler.neutronRoute.notFound");
        }

        CompanionRuntime.narrator().announce(outcome, false);
        return null;
    }

    /**
     * Reads one stated flag; absent counts as false, so a commander who never mentioned it gets the plain
     * route rather than a question back.
     */
    static boolean readFlag(JsonObject params, String name) {
        JsonElement element = params == null ? null : params.get(name);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    /**
     * Reads one stated number, or {@code null} when the commander did not state it or the value makes no
     * sense for this parameter. Out-of-range counts as unstated rather than as an error: the commander gave
     * an order, and a misheard digit is no reason to hand it back to them.
     */
    static Integer readNumber(JsonObject params, String name, int min, int max) {
        JsonElement element = params.get(name);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        Integer value = parseWholeNumber(element.getAsString());
        return value == null || value < min || value > max ? null : value;
    }

    /**
     * Reads a number a model may have written any of several ways.
     * <p>
     * A decimal is parsed as a decimal first, because these arrive as JSON numbers and a model that answers
     * {@code 6.0} means six: stripping the punctuation out of the digits, which is what {@code getIntSafely}
     * does, would read that as sixty and quietly plot a route nobody asked for. {@code getIntSafely} still
     * catches the wordier forms - "70 percent" - that are not a number at all.
     */
    static Integer parseWholeNumber(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException e) {
            return getIntSafely(raw);
        }
    }

    /**
     * What we say while the plot runs. The supercharge is read back only when it is on: a commander who
     * did not raise it is owed the plain line, not a confirmation of a setting they never chose.
     */
    private static String calculatingLine(String origin, String destination, int efficiency, boolean supercharge) {
        String key = supercharge
                ? "handler.neutronRoute.calculatingWithSupercharge"
                : "handler.neutronRoute.calculating";
        return StringUtls.localizedResponse(key, origin, destination, efficiency);
    }
}
