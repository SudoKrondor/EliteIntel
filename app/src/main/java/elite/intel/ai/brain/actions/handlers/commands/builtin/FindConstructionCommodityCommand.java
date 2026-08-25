package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.carrier.CarrierSupply;
import elite.intel.gameapi.carrier.OwnCarrierHold;
import elite.intel.gameapi.colonisation.ActiveConstructionSite;
import elite.intel.gameapi.colonisation.ConstructionCargo;
import elite.intel.gameapi.colonisation.ManifestAge;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.commodity.WantedCommodity;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * "Find what the construction site needs" - the sibling of {@link FindMissionCommodityCommand} that reads
 * the commodity off a colonisation depot's manifest instead of the mission board.
 * <p>
 * A colonisation build wants thousands of tonnes across a dozen-odd commodities, far more than any hold
 * carries, so this is asked once per trip and picks the <b>largest shortfall</b>: Steel at 2542 tonnes is
 * many runs in any ship, while the nine tonnes of Fruit and Vegetables at the bottom of the manifest will
 * fit in the corner of a hold on some later one. Working the long pole first is what shortens the job.
 * <p>
 * <b>Why the gate is the build, not the pad.</b> This and {@link FindCommodityCommand} are near-identical
 * requests in plain language - "find me some steel" versus "find steel for the site" - so something has to
 * keep a classifier from taking an ordinary trade errand for a colonisation one. That something is having a
 * live build on the go: a commander who is not hauling to one never sees this offered at all.
 * <p>
 * It was originally narrower still - offered only while standing on the depot's own pad - on the reasoning
 * that the manifest is only readable from there. That reasoning confused reading the manifest with using
 * it. The manifest is stored, and the question "what does the build still need, and where do I get it"
 * belongs to the shopping run, which by definition happens somewhere else: at a market, or parked on your
 * own carrier with the materials already aboard. Gated on the pad, the one moment the commander could ask
 * was the one moment the answer was useless, because they were standing on the place they wanted the cargo
 * delivered to. Same reasoning as {@code AnalyzeConstructionSiteQuery}, which has always answered off the
 * stored manifest.
 * <p>
 * The figures are therefore a claim about the last visit, and are spoken as one - see
 * {@link #sourcingCaveat}.
 * <p>
 * The site need not be the commander's own. Any commander can haul to any depot, and the site this was
 * built against belongs to another architect entirely.
 */
@RegisterCommand
public final class FindConstructionCommodityCommand implements IntelCommand {
    public static final String ID = "find_construction_site_commodity";

    private static final String PARAM_MAX_DISTANCE = "max_distance";
    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private final ConstructionSiteManager constructionSiteManager = ConstructionSiteManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String llmDescription() {
        return "Find where to get the cargo the colonisation construction site the commander is hauling to "
                + "still needs, and plot a route to it. Checks the commander's own fleet carrier before any "
                + "market. Takes NO commodity name: it reads the build's own manifest and picks the "
                + "commodity with the largest outstanding tonnage that is not already in the hold. Use this "
                + "whenever the wanted cargo is described as construction, colonisation or build-site cargo "
                + "or materials, wherever the ship happens to be. For a commodity the commander NAMES, use "
                + "the ordinary commodity search instead.";
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum galactic search radius in light years (ly). If omitted, a default range is used.",
                List.of("80", "150"),
                "Extract the distance limit in light years if the commander states one, ALWAYS as digits: "
                        + "the 80 in 'find construction cargo within 80 ly', and 200 for 'within two hundred light years'.");
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

    /**
     * Whenever there is a live build that still wants something - see the class note on why the gate is the
     * build rather than the pad. Not gated on being docked, and deliberately so: the shopping run is flown,
     * and "what does the build need next" is a question asked on the way out.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        Site site = siteWeAreHaulingTo();
        return site != null && constructionSiteManager.hasOutstandingRequirements(site.getMarketId());
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Site site = siteWeAreHaulingTo();
        if (site == null) {
            return StringUtls.localizedResponse("query.construction.noSite");
        }

        JsonElement stateEl = params == null ? null : params.get(PARAM_STATE);
        boolean returnClosest = stateEl != null && !stateEl.isJsonNull() && stateEl.getAsBoolean();
        int distance = GetNumberFromParam.extractRangeParameter(params, CommodityPurchaseSearch.defaultRange()).intValue();

        List<ConstructionCargo.Outstanding> manifest = ConstructionCargo.outstanding(
                constructionSiteManager.requirements(site.getMarketId()),
                ConstructionCargo.heldBySymbol(playerSession.getShipCargo()));

        if (manifest.isEmpty()) {
            return StringUtls.localizedResponse("handler.construction.complete", siteName(site));
        }
        // Only what the hold does not already cover, so the head of the list is the good this trip is for.
        List<ConstructionCargo.Outstanding> stillToBuy = manifest.stream()
                .filter(line -> !line.isSatisfied())
                .toList();
        if (stillToBuy.isEmpty()) {
            // The hold already covers everything the site still wants. That is an answer, and a useful
            // one: it means fly back and unload, not go shopping.
            return StringUtls.localizedResponse("handler.construction.holdCovers", siteName(site));
        }

        ConstructionCargo.Outstanding outstanding = stillToBuy.getFirst();
        String commodity = commodityName(outstanding);
        if (commodity == null) {
            return StringUtls.localizedResponse("handler.construction.unknownCommodity",
                    outstanding.gameName() == null ? outstanding.symbol() : outstanding.gameName());
        }

        // Spoken in the commander's language, not the game's: the manifest names the good in whatever
        // language the game client runs in, and the search needs it in English regardless.
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse(
                "handler.construction.sourcing",
                outstanding.shortfall(),
                FuzzySearch.localizedCommodityName(commodity),
                siteName(site)) + sourcingCaveat(site), false);

        // The WHOLE outstanding list, not just the largest line. The search is still anchored on that line -
        // it is the reason for the trip - but a build's long tail is nine or ten commodities of sixty tonnes
        // each, and a hold that can take all of them in one run should. The shortfalls, not the site's full
        // requirements: asking Spansh for 2542 tonnes of steel would pass over every market that can fill
        // this ship.
        List<WantedCommodity> shoppingList = shoppingList(stillToBuy, commodity);

        // Our own carrier before anyone's market. This is the whole point of hauling with a carrier:
        // stockpile the materials, park it at the build, and shuttle. Sending the commander to buy steel
        // they already own in the next orbit is the wrong answer, and it is what they got before this.
        String fromOurOwnHold = collectFromCarrier(shoppingList);
        if (fromOurOwnHold != null) {
            return fromOurOwnHold;
        }

        return CommodityPurchaseSearch.findBasketAndPlot(shoppingList, distance, returnClosest);
    }

    /**
     * Sends the commander to their own carrier when it can supply what the build needs, or returns null to
     * let the market search run.
     * <p>
     * Deliberately construction-only. Every other commodity search means "where can I BUY this", and the
     * commander's own hold is not a market - answering "you already have some" to a purchase question would
     * be a non-sequitur. A construction run is different: the goods only have to reach the depot, and where
     * they come from is exactly what the commander wants decided for them.
     * <p>
     * Both carriers are weighed, not just the first one carrying anything: a fleet carrier holding leftover
     * trade goods must not mask a squadron carrier loaded with the materials being asked about.
     */
    private String collectFromCarrier(List<WantedCommodity> shoppingList) {
        String currentSystem = playerSession.getPrimaryStarName();
        int holdCapacity = CommodityPurchaseSearch.holdCapacity();

        Optional<CarrierSupply.Loaded> best =
                CarrierSupply.best(OwnCarrierHold.ours(), shoppingList, holdCapacity, currentSystem);
        if (best.isEmpty()) return null;

        CarrierSupply.Loaded carrier = best.get();
        String goods = carrier.loadable().stream()
                .map(line -> StringUtls.localizedResponse("handler.commodity.basketItem",
                        line.unitsToLoad(), FuzzySearch.localizedCommodityName(line.commodity())))
                .collect(Collectors.joining(", "));

        if (!CarrierSupply.worthGoing(carrier.carrier(), carrier.loadable(), holdCapacity, currentSystem)) {
            // Said out loud rather than silently discarded: the commander is the one who knows whether a few
            // tonnes justifies the jumps, and cannot weigh it against a market run they were never told
            // about. The search carries on outward regardless.
            CompanionRuntime.narrator().filler(StringUtls.localizedResponse(
                    "handler.construction.carrierTooFar",
                    carrier.carrier().callSign(), goods, carrier.carrier().starSystem()), false);
            return null;
        }

        if (carrier.here()) {
            // Nothing to plot: the carrier is in this system, which is a supercruise hop rather than a
            // route. Saying so is the answer.
            return withSnapshotAge(carrier, StringUtls.localizedResponse("handler.construction.carrierHere",
                    carrier.carrier().callSign(), goods, carrier.tonnes()));
        }

        String answer = withSnapshotAge(carrier, StringUtls.localizedResponse(
                "handler.construction.carrierElsewhere",
                carrier.carrier().callSign(), carrier.carrier().starSystem(), goods, carrier.tonnes()));
        ReminderManager.getInstance().setReminder(answer, carrier.carrier().starSystem(),
                carrier.carrier().callSign(), null);
        return new RoutePlotter().plotRouteAnd(answer, carrier.carrier().starSystem());
    }

    /**
     * Says how old our look at the carrier's shelves is, when it is old enough to matter.
     * <p>
     * The game reports a carrier's cargo exactly once, while the commander is standing in its market, and
     * never mentions anything moved on or off afterwards. An old snapshot is therefore a claim about the
     * past, and the commander is the only one who can judge whether it still holds - so they are told rather
     * than sent somewhere on a flat assertion. Same principle as speaking only measured carrier fuel flatly.
     */
    private static String withSnapshotAge(CarrierSupply.Loaded carrier, String answer) {
        if (!CarrierSupply.snapshotIsStale(carrier.carrier())) return answer;
        return answer + " " + StringUtls.localizedResponse("handler.construction.carrierSnapshotOld",
                ManifestAge.hoursSince(carrier.carrier().seenAt().toString()));
    }

    /**
     * The manifest as a shopping list, largest shortfall first, with the anchor at the head.
     * <p>
     * Lines whose commodity cannot be named in the commodities table's spelling are dropped: Spansh matches
     * on that name, so a line it cannot be given is a line no market can be searched for. The anchor is
     * passed in already resolved because it has a fallback the others do not need - it is the one line the
     * commander is actually being sent for.
     *
     * @param stillToBuy the outstanding lines the hold does not already cover, largest shortfall first;
     *                   the head of this list is the anchor, which is why the caller filters before calling
     */
    static List<WantedCommodity> shoppingList(List<ConstructionCargo.Outstanding> stillToBuy, String anchorCommodity) {
        List<WantedCommodity> wanted = new ArrayList<>();
        ConstructionCargo.Outstanding anchor = stillToBuy.getFirst();
        wanted.add(new WantedCommodity(anchor.symbol(), anchorCommodity, anchor.shortfall()));
        for (ConstructionCargo.Outstanding line : stillToBuy.subList(1, stillToBuy.size())) {
            String commodity = FuzzySearch.commodityNameForSymbol(line.symbol());
            if (commodity == null) continue;
            wanted.add(new WantedCommodity(line.symbol(), commodity, line.shortfall()));
        }
        return wanted;
    }

    /**
     * The English name the commodities table and Spansh both spell it with, or null for a good neither
     * knows - falling back to the game's own localised name, which matches only when the app and the game
     * are set to the same language but is the one other handle we have.
     */
    private static String commodityName(ConstructionCargo.Outstanding outstanding) {
        String commodity = FuzzySearch.commodityNameForSymbol(outstanding.symbol());
        if (commodity != null) return commodity;
        if (outstanding.gameName() == null || outstanding.gameName().isBlank()) return null;
        return FuzzySearch.fuzzyCommodityMatch(outstanding.gameName(), 3);
    }

    /**
     * The build this question is about, or null when the commander is not on one.
     * <p>
     * The pad under the ship wins when it is a depot, because standing on one is unambiguous - the commander
     * can haul to several builds, and answering about the wrong one is worse than declining to answer.
     * Anywhere else it is the build they were last standing on, which is the same site the HUD card and
     * {@code NavigateToConstructionSiteCommand} mean by "the construction site". Held to
     * {@link ActiveConstructionSite#isLive} there and not on the pad: a manifest days old is too stale to
     * send someone shopping on, while landing at the depot has just rewritten it.
     */
    private Site siteWeAreHaulingTo() {
        long marketId = currentMarketId();
        Site onThisPad = marketId == 0 ? null : constructionSiteManager.findSite(marketId);
        if (onThisPad != null) return onThisPad;
        Site current = constructionSiteManager.currentSite();
        return ActiveConstructionSite.isLive(current) ? current : null;
    }

    /**
     * Says how old the manifest we are shopping against is, when it is old enough to matter, and nothing at
     * all on the pad that just wrote it.
     * <p>
     * Other commanders deliver to the same depot, so an hour-old manifest is a claim about the past, and
     * stating it flatly is how a commander buys six hundred tonnes of something that was finished while they
     * were away. Same rule the progress query and the HUD card already follow, and the same reason the
     * carrier's shelves carry their own caveat.
     */
    private String sourcingCaveat(Site site) {
        long hours = ManifestAge.hoursSince(site.getVisitedAt());
        if (hours < 1) return "";
        return " " + StringUtls.localizedResponse("handler.construction.manifestAsOf", hours);
    }

    /**
     * The port the ship is standing on, as the journal named it.
     * <p>
     * WHY not the location tables: they are keyed by {@code (systemAddress, bodyId)} and {@code Docked}
     * carries no bodyId, so the lookup misses and hands back a fresh row whose MarketID is zero. Gating on
     * that made this command invisible at a depot whose manifest the app was reading correctly at the same
     * moment - see {@link DockedMarket}.
     */
    private long currentMarketId() {
        return DockedMarket.getInstance().marketId();
    }

    /**
     * The depot's name, or a generic word for it when the {@code Docked} that named it was never seen -
     * after a restart on the pad, say.
     */
    private static String siteName(Site site) {
        String name = site.getStationName();
        return name == null || name.isBlank()
                ? StringUtls.localizedResponse("handler.construction.thisSite")
                : name;
    }
}
