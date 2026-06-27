package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.edsm.commodity.CommoditySearchResult;
import elite.intel.search.edsm.commodity.EdsmCommoditySearch;
import elite.intel.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import java.util.List;

import static elite.intel.util.StringUtls.capitalizeWords;
import static elite.intel.util.StringUtls.getIntSafely;

/**
 * Self-describing "find commodity" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindCommodityHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindCommodityCommand implements IntelCommand {
    public static final String ID = "find_commodity";

    @Override public String llmDescription() { return "Find where a commodity can be bought or sold nearby."; }


    private static final String PARAM_KEY = "key";
    private static final String PARAM_MAX_DISTANCE = "max_distance";
    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final TradeProfileManager tradeProfileManager = TradeProfileManager.getInstance();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The commodity (market good) to search for, e.g. gold, tritium, painite.",
                List.of("gold", "tritium"),
                "Extract the commodity name verbatim in lower case; do not translate.");
        key.validate();
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum galactic search radius in light years (ly). If omitted, a default range is used.",
                List.of("80", "150"),
                "Extract the distance limit in light years if the commander states one (e.g. the 80 in 'find gold within 80 ly').");
        maxDistance.validate();
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE, "boolean", false,
                "Search mode: true = nearest market (by distance); false = best price / where to buy.",
                List.of("true", "false"),
                "Set true when the commander says 'nearest' or 'closest'; otherwise false.");
        state.validate();
        return List.of(key, maxDistance, state);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        JsonElement key = params.get(PARAM_KEY);
        JsonElement maxGalacticDistance = params.get(PARAM_MAX_DISTANCE);
        JsonElement stateEl = params.get(PARAM_STATE);
        boolean returnClosest = stateEl != null && stateEl.getAsBoolean();
        Integer distance = maxGalacticDistance == null ? null : getIntSafely(maxGalacticDistance.getAsString());
        if (distance == null || distance < 1) distance = (int) playerSession.getShipLoadout().getMaxJumpRange() * 2;
        String starName = playerSession.getPrimaryStarName();

        if (key == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.commodity.specify")));
            return;
        }

        String commodity =
                capitalizeWords(
                        FuzzySearch.fuzzyCommodityMatch(
                                key.getAsString(), 3
                        )
                );

        if (commodity == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.commodity.notFound", key.getAsString())));
            return;
        }

        String searchMode = StringUtls.localizedLlm(returnClosest ? "handler.commodity.modeNearest" : "handler.commodity.modeBest");
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.commodity.searching", searchMode, commodity, distance)));
        TradeRouteSearchCriteria tradeProfileManagerCriteria = tradeProfileManager.getCriteria(false);
        int cargoCapacity = tradeProfileManagerCriteria.getMaxCargo();
        if (cargoCapacity == 0) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.commodity.noCargoCapacity")));
            return;
        }
        int maxDistanceFromArrival = tradeProfileManagerCriteria.getMaxLsFromArrival();
        if (maxDistanceFromArrival == 0) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.commodity.maxDistanceFromArrivalNoSet")));
            return;
        }
        List<CommoditySearchResult> results = EdsmCommoditySearch.search(
                commodity,
                starName,
                distance,
                maxDistanceFromArrival,
                cargoCapacity,
                returnClosest
        );
        if (results.isEmpty()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.commodity.noMatch")));
            return;
        }
        ReminderManager reminderManager = ReminderManager.getInstance();
        CommoditySearchResult result = results.getFirst();
        String reminder = StringUtls.localizedLlm("handler.commodity.headTo", result.getStarSystem(), result.getStationName(), result.getStationType(), result.getPrice());
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(reminder));
        reminderManager.setReminder(reminder, result.getStarSystem());

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(result.getStarSystem());
    }
}
