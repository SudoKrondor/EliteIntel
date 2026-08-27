package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MissionCargo;
import elite.intel.gameapi.search.spansh.commodity.WantedCommodity;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * "Find the cargo my mission needs" - the sibling of {@link FindCommodityCommand} that reads the
 * commodity off the mission board instead of out of the commander's sentence.
 * <p>
 * Source-and-return missions are taken in stacks and can only be flown one at a time, so the command
 * has to choose: {@link MissionCargo} picks the mission expiring soonest whose cargo is not already in
 * the hold, which is the order the stack has to be worked in anyway. From there it is the same search
 * the commander gets by naming a good, via {@link CommodityPurchaseSearch}.
 * <p>
 * The mission's own {@code Commodity_Localised} is deliberately not what is searched for. It is written
 * in the language the GAME is running in - one of six - while the app speaks nine, and Spansh matches
 * only the English name. The language-free symbol resolves to that English name through the commodities
 * table, so the search works whatever pair of languages the commander has set.
 */
@RegisterCommand
public final class FindMissionCommodityCommand implements IntelCommand {
    public static final String ID = "find_mission_commodity";

    @Override
    public String llmDescription() {
        return "Find where to buy the cargo an active mission still needs, and plot a route to it. Takes NO "
                + "commodity name: it reads the requirement off the mission board, picking the mission that "
                + "expires soonest whose cargo is not already in the hold. Use this when the commander asks "
                + "about mission cargo rather than naming a good.";
    }

    private static final String PARAM_MAX_DISTANCE = "max_distance";
    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private final MissionManager missionManager = MissionManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum galactic search radius in light years (ly). If omitted, a default range is used.",
                List.of("80", "150"),
                "Extract the distance limit in light years if the commander states one, ALWAYS as digits: "
                        + "the 80 in 'find mission cargo within 80 ly', and 200 for 'within two hundred light years'.");
        maxDistance.validate();
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE, "boolean", false,
                "Search mode: true = nearest market (by distance); false = best price / where to buy.",
                List.of("true", "false"),
                "Set true when the commander says 'nearest' or 'closest'; otherwise false.");
        state.validate();
        return List.of(maxDistance, state);
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
        JsonElement stateEl = params == null ? null : params.get(PARAM_STATE);
        boolean returnClosest = stateEl != null && !stateEl.isJsonNull() && stateEl.getAsBoolean();
        int distance = GetNumberFromParam.extractRangeParameter(params, CommodityPurchaseSearch.defaultRange()).intValue();

        List<MissionCargo.Outstanding> board = MissionCargo.outstanding(
                withCommoditySymbols(missionManager.getMissions().values()),
                MissionCargo.heldBySymbol(playerSession.getShipCargo(), playerSession.getSuitInventory()));

        if (board.isEmpty()) {
            return StringUtls.localizedResponse("handler.missionCommodity.noCargoMissions");
        }
        // Only what the hold does not already cover, so the head of the list is the mission this trip is for.
        List<MissionCargo.Outstanding> stillToBuy = board.stream()
                .filter(item -> !item.isSatisfied())
                .toList();
        if (stillToBuy.isEmpty()) {
            // Every requirement covered is a real answer and a useful one - it means fly, not shop.
            return StringUtls.localizedResponse("handler.missionCommodity.allAcquired");
        }

        MissionCargo.Outstanding outstanding = stillToBuy.getFirst();
        String commodity = FuzzySearch.commodityNameForSymbol(outstanding.symbol());
        if (commodity == null) {
            // A legacy or Powerplay good the commodities table carries no symbol for. The game's own
            // localised name is the only other handle we have, and it matches only when the app and the
            // game are set to the same language - worth trying before giving up on the commander.
            commodity = FuzzySearch.fuzzyCommodityMatch(outstanding.mission().getCommodityName(), 3);
        }
        if (commodity == null) {
            return StringUtls.localizedResponse("handler.missionCommodity.unknownCommodity",
                    outstanding.mission().getCommodityName());
        }

        // Said in the commander's language, not the game's: the mission named the good in whatever
        // language the game is running in, and the search needs it in English regardless.
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse(
                "handler.missionCommodity.sourcing",
                outstanding.shortfall(),
                FuzzySearch.localizedCommodityName(commodity),
                outstanding.mission().getFaction()), false);

        // The whole board, not just the mission at the head of it. A stack of source-and-return missions is
        // taken all at once and flown one at a time; a hold with room for two of them should carry two. The
        // search stays anchored on the soonest to expire, so the trip is still built around the deadline.
        // Shortfalls rather than full counts: after a part load this asks for what is still missing, so the
        // remainder can be picked up somewhere that only has that much.
        return CommodityPurchaseSearch.findBasketAndPlot(shoppingList(stillToBuy, commodity), distance, returnClosest);
    }

    /**
     * The board as a shopping list, soonest expiry first, with the anchor at the head.
     * <p>
     * A mission whose commodity cannot be named in the commodities table's spelling is dropped: Spansh
     * matches on that name. Only the anchor gets the localised-name fallback, because it is the one mission
     * the commander is actually being sent for and the only one worth failing the whole search over.
     *
     * @param stillToBuy outstanding requirements the hold does not already cover, soonest expiry first
     */
    static List<WantedCommodity> shoppingList(List<MissionCargo.Outstanding> stillToBuy, String anchorCommodity) {
        List<WantedCommodity> wanted = new ArrayList<>();
        MissionCargo.Outstanding anchor = stillToBuy.getFirst();
        wanted.add(new WantedCommodity(anchor.symbol(), anchorCommodity, anchor.shortfall()));
        for (MissionCargo.Outstanding item : stillToBuy.subList(1, stillToBuy.size())) {
            String commodity = FuzzySearch.commodityNameForSymbol(item.symbol());
            if (commodity == null) continue;
            wanted.add(new WantedCommodity(item.symbol(), commodity, item.shortfall()));
        }
        return wanted;
    }

    /**
     * Fills in the symbol for missions accepted before it was being recorded, which carry only the
     * game's localised name.
     * <p>
     * Resolving the name back to a symbol works whenever the app and the game are set to the same
     * language - the common case - and costs one table lookup per mission. Without it, a board taken
     * before this release reads as "no mission needs cargo" until the last of those missions has
     * expired. The repair is in memory only; the next accepted mission records the symbol properly.
     */
    private Collection<MissionDto> withCommoditySymbols(Collection<MissionDto> missions) {
        for (MissionDto mission : missions) {
            if (mission == null || mission.getCommoditySymbol() != null) continue;
            String name = mission.getCommodityName();
            if (name == null || name.isBlank()) continue;
            String english = FuzzySearch.fuzzyCommodityMatch(name, 3);
            mission.setCommoditySymbol(JournalSymbol.normalize(FuzzySearch.commoditySymbol(english)));
        }
        return missions;
    }
}
