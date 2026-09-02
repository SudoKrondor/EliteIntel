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
import elite.intel.gameapi.colonisation.ActiveConstructionSite;
import elite.intel.gameapi.colonisation.CarrierStockpile;
import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ConstructionCargo;
import elite.intel.gameapi.colonisation.ConstructionShopping;
import elite.intel.gameapi.colonisation.ManifestAge;
import elite.intel.gameapi.search.spansh.commodity.WantedCommodity;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
 * <b>The carrier is subtracted, not reported.</b> A commander hauling with a carrier stockpiles the
 * materials on it first, so a good part of the manifest is usually bought already and sitting in their own
 * hold - and a search that ignored that sent them to buy 1,858 tonnes of Aluminium with 1,760 of them in
 * the next orbit. But the question asked is where to BUY, and what is aboard is on the HUD card in front of
 * them: reciting it back was chatter that buried the one figure they wanted. So the carrier's stock comes
 * off every tonnage this speaks and is never named, with one exception - when it covers the whole of what
 * is left there is nothing to buy at all, and saying so IS the answer.
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
        return "Find where to BUY the cargo the colonisation construction site the commander is hauling to "
                + "still needs, and plot a route to that market. Takes NO commodity name: it reads the "
                + "build's own manifest and picks the commodity with the largest tonnage still to buy, "
                + "counting what is already in the ship's hold and on the commander's own carrier. Use this "
                + "whenever the wanted cargo is described as construction, colonisation or build-site cargo "
                + "or materials, wherever the ship happens to be. For a commodity the commander NAMES, use "
                + "the ordinary commodity search instead. This answers WHERE to buy, never HOW MUCH: a "
                + "question asking how much, how many tonnes, or how much is left to buy wants a figure off "
                + "the build's manifest, not a search - report construction site progress for those.";
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
        int distance = GetNumberFromParam.extractRangeParameter(params, CommodityTradeSearch.defaultRange()).intValue();

        List<ConstructionCargo.Outstanding> manifest = ConstructionCargo.outstanding(
                constructionSiteManager.requirements(site.getMarketId()),
                ConstructionCargo.heldBySymbol(playerSession.getShipCargo()));

        if (manifest.isEmpty()) {
            return StringUtls.localizedResponse("handler.construction.complete", siteName(site));
        }
        // Nothing left once the SHIP'S hold is counted means the cargo is already moving: fly it back, do
        // not go shopping. Settled before the carrier is consulted, so that the two "nothing to buy"
        // answers stay the different errands they are - and so that a list closed below can only have been
        // closed by the carrier.
        if (manifest.stream().allMatch(ConstructionCargo.Outstanding::isSatisfied)) {
            return StringUtls.localizedResponse("handler.construction.holdCovers", siteName(site));
        }

        // Our own carrier before anyone's market. This is the whole point of hauling with a carrier:
        // stockpile the materials, park it at the build, and shuttle. Tonnes already sitting on it are
        // bought and paid for, so telling the commander to buy them again is the wrong answer.
        //
        // The same stockpile the HUD card and the progress query count, deliberately: the card is where the
        // commander READS what is aboard, so a spoken figure worked out any other way would contradict the
        // screen in front of them.
        Stash stash = CarrierStockpile.forBuild(site, symbols(manifest)).orElse(null);
        List<ConstructionShopping.Line> stillToBuy =
                longPoleFirst(ConstructionShopping.stillToAcquire(manifest, stash));

        if (stillToBuy.isEmpty()) {
            // The hold was ruled out above, so a list closed here was closed by the carrier - which is the
            // reason the carrier is consulted at all. Nothing to search for, and nowhere to send them.
            return carrierCoversIt(stash, site);
        }

        // What is aboard the carrier is NOT said out loud. The commander asked where to BUY, the card
        // already shows the stockpile, and reciting it was the chatter that buried the answer. It earns its
        // place here by being subtracted from the tonnages below, which is what they act on.
        //
        // The WHOLE remaining list, not just the largest line. The search is still anchored on that line -
        // it is the reason for the trip - but a build's long tail is nine or ten commodities of sixty tonnes
        // each, and a hold that can take all of them in one run should. The shortfalls, not the site's full
        // requirements: asking Spansh for 2542 tonnes of steel would pass over every market that can fill
        // this ship.
        List<WantedCommodity> shoppingList = shoppingList(stillToBuy);
        if (shoppingList.isEmpty()) {
            ConstructionCargo.Outstanding unknown = stillToBuy.getFirst().good();
            return StringUtls.localizedResponse("handler.construction.unknownCommodity",
                    unknown.gameName() == null ? unknown.symbol() : unknown.gameName());
        }

        // Spoken in the commander's language, not the game's: the manifest names the good in whatever
        // language the game client runs in, and the search needs it in English regardless.
        WantedCommodity anchor = shoppingList.getFirst();
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse(
                "handler.construction.sourcing",
                anchor.unitsWanted(),
                FuzzySearch.localizedCommodityName(anchor.commodity()),
                siteName(site)) + sourcingCaveat(site), false);

        return CommodityTradeSearch.findBasketAndPlot(shoppingList, distance, returnClosest);
    }

    /**
     * Nothing to buy, because our own carrier is already holding all of it.
     * <p>
     * <b>Why this is not a route.</b> The commander asked where to BUY, and the answer is that they need
     * not: the cargo is theirs, and what to do with it - shuttle it, or jump the carrier out to the depot -
     * is a decision they make, not one a commodity search should make for them by plotting somewhere.
     * <p>
     * Deliberately construction-only. Every other commodity search means "where can I buy this", and a hold
     * we already own is not a market - answering "you have some" to a purchase question would be a
     * non-sequitur. A construction run is different: the goods only have to reach the depot, so where they
     * come from is exactly what the commander wants settled.
     */
    private static String carrierCoversIt(Stash stash, Site site) {
        String answer = stash.starSystem() == null || stash.starSystem().isBlank()
                ? StringUtls.localizedResponse("handler.construction.carrierCoversIt",
                siteName(site), stash.callSign())
                : StringUtls.localizedResponse("handler.construction.carrierCoversItAt",
                siteName(site), stash.callSign(), stash.starSystem());
        if (!stash.snapshotIsStale()) return answer;
        // Said out loud rather than asserted flatly: the game reports a carrier's cargo exactly once, in its
        // own market, and never mentions what anyone bought off its sell orders afterwards. "Nothing left to
        // buy" off an old snapshot is how a commander flies to the depot short. Same principle as speaking
        // only measured carrier fuel flatly.
        return answer + " " + StringUtls.localizedResponse("handler.construction.carrierSnapshotOld",
                ManifestAge.hoursSince(stash.seenAt().toString()));
    }

    /**
     * Every commodity the site still wants, as bare journal symbols - which carrier is working this build is
     * decided against the whole manifest, exactly as the HUD card decides it.
     */
    private static Set<String> symbols(List<ConstructionCargo.Outstanding> manifest) {
        return manifest.stream()
                .map(ConstructionCargo.Outstanding::symbol)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The shopping list in the order a trip is planned in: the largest tonnage STILL TO BUY first.
     * <p>
     * Not the order {@link ConstructionShopping} hands back, which is the site's total requirement and is
     * fixed that way on purpose - the HUD card must not reshuffle under the commander's hands while they
     * shop. A search has the opposite need: its anchor is the good this trip is actually for, and after the
     * carrier is counted that is whatever is left in the largest quantity. A build wanting 1,858 tonnes of
     * Aluminium with 1,760 of them already on the carrier is a 98 tonne errand, and must not out-rank the
     * 600 tonnes of Steel nobody has bought yet.
     */
    static List<ConstructionShopping.Line> longPoleFirst(List<ConstructionShopping.Line> lines) {
        return lines.stream()
                .sorted(Comparator
                        .comparingInt(ConstructionShopping.Line::stillToBuy).reversed()
                        // A stable tie-break, so the same manifest always names the same commodity.
                        .thenComparing(ConstructionShopping.Line::symbol))
                .toList();
    }

    /**
     * The remaining lines as a shopping list, in the order they were given.
     * <p>
     * Lines whose commodity cannot be named in the commodities table's spelling are dropped: Spansh matches
     * on that name, so a line it cannot be given is a line no market can be searched for. An empty result
     * therefore means the whole of what is left is unnameable, which the caller reports rather than
     * searching for nothing.
     */
    static List<WantedCommodity> shoppingList(List<ConstructionShopping.Line> stillToBuy) {
        List<WantedCommodity> wanted = new ArrayList<>();
        for (ConstructionShopping.Line line : stillToBuy) {
            String commodity = commodityName(line.good());
            if (commodity == null) continue;
            wanted.add(new WantedCommodity(line.symbol(), commodity, line.stillToBuy()));
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
