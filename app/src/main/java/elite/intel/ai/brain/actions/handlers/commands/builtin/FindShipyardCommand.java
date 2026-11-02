package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipMakeManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.ReminderContact;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.station.SearchRadii;
import elite.intel.gameapi.search.spansh.station.shipyard.ShipyardHit;
import elite.intel.gameapi.search.spansh.station.shipyard.ShipyardSearch;
import elite.intel.gameapi.search.spansh.station.shipyard.ShipyardShip;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.TTSFriendlyNumberConverter;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;
import java.util.stream.Collectors;

/**
 * "Find a shipyard where I can buy a Cobra MkIII, and take me there."
 *
 * <p>Replaces the old query about the shipyard the commander is docked at, which could only answer once the
 * shipyard screen was already open in front of him - that is when the game writes its contents to the journal.
 *
 * <p>Gated on the word "shipyard" ({@code ai_action_cues}): "where can I buy X" stays the commodity and module
 * search's sentence, and this one is offered only when the commander says where he wants to buy it.
 */
@RegisterCommand
public final class FindShipyardCommand implements IntelCommand {
    public static final String ID = "find_shipyard_selling_ship";

    private static final String PARAM_KEY = "key";
    private static final String PARAM_MAX_DISTANCE = "max_distance";
    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    @Override
    public String llmDescription() {
        return "Find a shipyard selling the ship (hull) in 'key' within 'max_distance' ly and plot a route to it; "
                + "'state' true = nearest shipyard, false = cheapest.";
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The ship to buy, e.g. cobra mk iii, python, imperial cutter.",
                List.of("cobra mk iii", "python", "imperial cutter"),
                "Give the ship's English name as the game lists it, in lower case, keeping its mark: "
                        + "'cobra mark three' is 'cobra mk iii'.");
        key.validate();
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum galactic search radius in light years (ly). If omitted, a default range is used.",
                List.of("80", "150"),
                "Extract the distance limit in light years if the commander states one, ALWAYS as digits.");
        maxDistance.validate();
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE, "boolean", false,
                "Search mode: true = nearest shipyard; false = cheapest.",
                List.of("true", "false"),
                "Set false when the commander says 'cheapest' or 'best price'; otherwise true.");
        state.validate();
        return List.of(key, maxDistance, state);
    }

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
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement key = params.get(PARAM_KEY);
        if (key == null || key.isJsonNull() || key.getAsString().isBlank()) {
            return StringUtls.localizedResponse("handler.shipyard.specify");
        }
        // Nearest unless cheapest was asked for: a hull costs the same everywhere a Power's discount does not
        // reach, so the price is rarely worth a longer trip.
        JsonElement stateEl = params.get(PARAM_STATE);
        boolean returnClosest = stateEl == null || stateEl.isJsonNull() || stateEl.getAsBoolean();
        int distance = GetNumberFromParam.extractRangeParameter(params, CommodityTradeSearch.defaultRange()).intValue();

        String spoken = key.getAsString();
        return switch (SpokenShip.resolve(spoken)) {
            case SpokenShip.One(ShipyardShip ship) -> findAndPlot(ship, distance, returnClosest);
            case SpokenShip.Several(List<ShipyardShip> ships) ->
                    StringUtls.localizedResponse("handler.shipyard.whichOne",
                            ships.stream().map(FindShipyardCommand::spokenName).collect(Collectors.joining(", ")));
            case SpokenShip.None() -> StringUtls.localizedResponse("handler.shipyard.notFound", spoken);
        };
    }

    /**
     * Finds the shipyard to buy {@code ship} at and plots a route to it.
     *
     * @return a localized message when the search could not be run, found nothing, or ended without a new route
     * being plotted
     */
    private static String findAndPlot(ShipyardShip ship, int distance, boolean returnClosest) {
        LocationDao.Coordinates coordinates = LocationManager.getInstance().getGalacticCoordinates();
        if (coordinates == null) {
            return StringUtls.localizedResponse("handler.module.noCoords");
        }
        String name = spokenName(ship);
        String searchMode = StringUtls.localizedResponse(returnClosest ? "handler.module.modeNearest" : "handler.module.modeBest");
        VegaRuntime.narrator().filler(StringUtls.localizedResponse("handler.shipyard.searching", name, distance, searchMode), false);

        // The pad rule every station search applies: the commander has to dock the ship he is flying to buy one.
        List<ShipyardHit> found = ShipyardSearch.find(ship, coordinates.x(), coordinates.y(), coordinates.z(),
                PlayerSession.getInstance().currentStarSystem(), distance,
                ShipManager.getInstance().requireLargePad(), returnClosest);
        if (found.isEmpty()) {
            return StringUtls.localizedResponse("handler.shipyard.noMatch", name, SearchRadii.widening(distance).getLast());
        }

        ShipyardHit hit = found.getFirst();
        String answer = hit.price() == null
                ? StringUtls.localizedResponse("handler.shipyard.headToUnpriced",
                hit.starSystem(), hit.stationName(), hit.stationType(), name)
                : StringUtls.localizedResponse("handler.shipyard.headTo",
                hit.starSystem(), hit.stationName(), hit.stationType(), name,
                TTSFriendlyNumberConverter.formatCreditsForSpeech(hit.price()));
        if (hit.distanceLy() > distance) {
            answer += " " + StringUtls.localizedResponse("handler.commodity.beyondRange", distance, Math.round(hit.distanceLy()));
        }
        VegaRuntime.narrator().filler(answer, false);

        ReminderManager.getInstance().setReminder(
                StringUtls.localizedResponse("handler.shipyard.reminder", name, hit.stationName(), hit.stationType()),
                hit.starSystem(), hit.stationName(), ReminderContact.SHIPYARD);

        return new RoutePlotter().plotRoute(hit.starSystem());
    }

    /**
     * The hull as the commander's game client names it, once the journal has said so; Spansh's English name until
     * then.
     */
    private static String spokenName(ShipyardShip ship) {
        String client = ShipMakeManager.getInstance().getDisplayName(ship.symbol());
        return client == null || client.isBlank() ? ship.name() : client;
    }
}
