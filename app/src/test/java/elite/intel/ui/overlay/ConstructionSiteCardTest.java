package elite.intel.ui.overlay;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ShoppingShelves.Shop;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static elite.intel.ui.overlay.HudCards.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The construction-site card: how far the build has got, and what to put in the hold on the next run.
 * <p>
 * The manifest is Orbital Construction Site: Divis Gateway's, which belongs to another architect - the
 * ordinary case, since anyone can haul to anyone's depot.
 */
class ConstructionSiteCardTest {

    private static final long MARKET_ID = 3967232514L;

    private static Requirement line(String symbol, int required, int provided) {
        Requirement requirement = new Requirement();
        requirement.setMarketId(MARKET_ID);
        requirement.setSymbol(symbol);
        requirement.setGameName(symbol);
        requirement.setRequiredAmount(required);
        requirement.setProvidedAmount(provided);
        requirement.setPayment(5057);
        return requirement;
    }

    private static List<Requirement> divisGateway() {
        return List.of(
                line("steel", 2542, 0),
                line("titanium", 1525, 0),
                line("computercomponents", 22, 22));
    }

    /**
     * The shape of market a stockpiling commander parks at: a port selling most of what a build eats.
     */
    private static List<Requirement> wideBuild() {
        return List.of(
                line("steel", 2542, 0), line("titanium", 1525, 0), line("copper", 900, 0),
                line("aluminium", 800, 0), line("polymers", 700, 0));
    }

    private static Site site(double progress, String visitedAt) {
        Site site = new Site();
        site.setMarketId(MARKET_ID);
        site.setStationName("Orbital Construction Site: Divis Gateway");
        site.setStarSystem("Hyades Sector NR-V b2-2");
        site.setProgress(progress);
        site.setVisitedAt(visitedAt);
        return site;
    }

    /**
     * Witt Hub's manifest as the game's construction panel showed it mid-shuttle: the REQUIRED column, with
     * steel already most of the way delivered.
     */
    private static List<Requirement> wittHub() {
        return List.of(
                line("steel", 8434, 6160), line("titanium", 7921, 880), line("water", 1609, 0),
                line("fruitandvegetables", 145, 0), line("waterpurifiers", 105, 0));
    }

    /**
     * A Type-9 sized hold, which is the ship this feature is flown in.
     */
    private static final int HOLD = 640;

    private static Optional<HudObjective> card(Site site, List<Requirement> manifest, Map<String, Integer> hold) {
        return card(site, manifest, hold, HOLD, Set.of());
    }

    private static Optional<HudObjective> card(Site site, List<Requirement> manifest,
                                               Map<String, Integer> hold, int capacity) {
        return card(site, manifest, hold, capacity, Set.of());
    }

    private static Optional<HudObjective> card(Site site, List<Requirement> manifest, Map<String, Integer> hold,
                                               int capacity, Set<String> stockedHere) {
        return new ConstructionSiteObjectiveSource(() -> site, marketId -> manifest, () -> hold,
                () -> capacity, () -> shop(stockedHere)).currentObjective();
    }

    /**
     * Papin's Inheritance in Sirius, which is where this build's shopping is done. An empty shelf is no
     * shop at all - in flight, or at a market whose screen was never opened.
     */
    private static final String SHOP = "Papin's Inheritance";

    private static Optional<Shop> shop(Set<String> stock) {
        return stock.isEmpty() ? Optional.empty()
                : Optional.of(new Shop(3223343616L, SHOP, "Sirius", stock));
    }

    /**
     * The same card read at a market with a carrier working this build - see {@code CarrierStockpileTest}
     * for which carrier that is.
     *
     * @param stockedHere what this market has on its shelves
     * @param stash       what the carrier is already holding for the build
     */
    private static Optional<HudObjective> shoppingCard(List<Requirement> manifest, Set<String> stockedHere,
                                                       Map<String, Integer> stash) {
        return shoppingCard(manifest, Map.of(), stockedHere, stash);
    }

    private static Optional<HudObjective> shoppingCard(List<Requirement> manifest, Map<String, Integer> hold,
                                                       Set<String> stockedHere, Map<String, Integer> stash) {
        return new ConstructionSiteObjectiveSource(() -> site(0.026187, Instant.now().toString()),
                marketId -> manifest, () -> hold, () -> HOLD, () -> shop(stockedHere),
                (ignored, alsoIgnored) -> Optional.of(new Stash("GHY-L8X", stash))).currentObjective();
    }

    /**
     * The shuttle run between a carrier and the depot: a carrier is working the build, and there is no
     * commodity market anywhere on the trip, so the card is the loading order rather than a shopping list.
     */
    private static Optional<HudObjective> shuttleCard(List<Requirement> manifest, Map<String, Integer> hold,
                                                      Map<String, Integer> stash) {
        return new ConstructionSiteObjectiveSource(() -> site(0.67271, Instant.now().toString()),
                marketId -> manifest, () -> hold, () -> 880, () -> shop(Set.of()),
                (ignored, alsoIgnored) -> Optional.of(new Stash("GHY-L8X", stash))).currentObjective();
    }

    @Test
    void theCardLeadsWithProgressAndWhatToLoad() {
        HudObjective objective = card(site(0.026187, Instant.now().toString()), divisGateway(), Map.of())
                .orElseThrow();

        assertEquals("CONSTRUCTION SITE", objective.title());
        assertEquals("ORBITAL CONSTRUCTION SITE: DIVIS GATEWAY", objective.subtitle());
        assertEquals(HudObjective.PRIORITY_STANDING, objective.priority());

        HudRow progress = rowOf(objective, "PROGRESS");
        assertTrue(progress.hasProgress(), "progress is a bar, not a number the commander has to read");
        assertEquals(3, progress.current());
        assertEquals(100, progress.max());

        assertEquals("640 T", valueOf(objective, "STEEL"),
                "steel alone fills the hold, so the trip is steel");
    }

    /**
     * The case that drove the change, from a live run: steel 622 short of a 640-tonne hold, and the search
     * fills the last 18 tonnes with titanium. The commander was told to buy both and shown only the steel.
     */
    @Test
    void aShortfallThatDoesNotFillTheHoldIsToppedUpWithTheNextGood() {
        List<Requirement> nearlyDone = List.of(
                line("steel", 2542, 1920),
                line("titanium", 1525, 1280),
                line("polymers", 170, 0));

        HudObjective objective = card(site(0.787978, Instant.now().toString()), nearlyDone, Map.of())
                .orElseThrow();

        assertEquals("622 T", valueOf(objective, "STEEL"));
        assertEquals("18 T", valueOf(objective, "TITANIUM"));
        assertFalse(labels(objective).contains("POLYMERS"), "the hold is full by then");
    }

    /**
     * One trip's worth next to the whole job. Without this the card would read as if 640 tonnes of steel
     * finished the build.
     */
    @Test
    void theCardAlsoSaysWhatTheWholeBuildIsStillShort() {
        HudObjective objective = card(site(0.026187, Instant.now().toString()), divisGateway(), Map.of())
                .orElseThrow();

        assertEquals("4.067 T", valueOf(objective, "OUTSTANDING").replace(',', '.'),
                "steel and titanium together; the satisfied line does not count");
    }

    /**
     * At most a handful of names, or the loading order turns into the manifest the spoken answer is for.
     */
    @Test
    void aLongTailIsCappedAtAHandfulOfGoods() {
        List<Requirement> longTail = List.of(
                line("steel", 60, 0), line("titanium", 50, 0), line("polymers", 40, 0),
                line("copper", 30, 0), line("superconductors", 20, 0));

        HudObjective objective = card(site(0.9, Instant.now().toString()), longTail, Map.of())
                .orElseThrow();

        assertTrue(labels(objective).contains("COPPER"));
        assertFalse(labels(objective).contains("SUPERCONDUCTORS"), "four goods is a loading order, five is a list");
    }

    /**
     * Before the game says which ship we are in there is no trip to describe, only a next commodity.
     */
    @Test
    void anUnknownHoldFallsBackToTheLargestShortfall() {
        HudObjective objective = card(site(0.026187, Instant.now().toString()), divisGateway(), Map.of(), 0)
                .orElseThrow();

        assertEquals("2.542 T", valueOf(objective, "STEEL").replace(',', '.'));
        assertFalse(labels(objective).contains("TITANIUM"));
    }

    /**
     * Cargo aboard is said on the good's OWN row. A single IN HOLD total told the commander they were
     * carrying 44 tonnes of something on the list - the one thing about it they could not act on.
     */
    @Test
    void cargoAlreadyAboardIsNamedOnTheGoodItBelongsTo() {
        HudObjective objective = card(site(0.026187, Instant.now().toString()), divisGateway(), Map.of("steel", 400))
                .orElseThrow();

        assertEquals("640 T +400", valueOf(objective, "STEEL"));
        assertEquals(HudRow.State.GOOD, rowOf(objective, "STEEL").state());
        assertFalse(labels(objective).contains("IN HOLD"), "there is no unattributed total any more");
        assertEquals("3.667 T", valueOf(objective, "OUTSTANDING").replace(',', '.'),
                "the shortfall is what is left to buy, with the hold already counted");
    }

    @Test
    void aGoodWeAreCarryingNoneOfIsNotMarkedGreen() {
        HudObjective objective = card(site(0.026187, Instant.now().toString()), divisGateway(), Map.of())
                .orElseThrow();

        assertEquals(HudRow.State.NORMAL, rowOf(objective, "STEEL").state());
    }

    /**
     * The card is read standing at a commodity screen. Measured at Fairfax Landing: of the four goods it
     * listed, one was on the shelves - the other three spent the card on things that could not be bought.
     */
    @Test
    void goodsThisPortActuallySellsComeFirst() {
        List<Requirement> tail = List.of(
                line("ceramiccomposites", 85, 0),
                line("water", 22, 0),
                line("superconductors", 60, 0),
                line("microcontrollers", 13, 0));

        HudObjective objective = card(site(0.88, Instant.now().toString()), tail, Map.of(), HOLD,
                Set.of("superconductors")).orElseThrow();

        assertEquals("SUPERCONDUCTORS", labels(objective).get(1),
                "the only one on the shelves leads, straight after the progress bar");
        assertEquals("CERAMIC COMPOSITES", labels(objective).get(2),
                "and the rest keep largest-shortfall order behind it");
    }

    /**
     * Witt Hub as the game's own construction panel shows it, read on the shuttle leg: steel 2,274 required,
     * titanium 7,041, water 1,609, and the small tail behind them. The commander wants their progress against
     * those same numbers, not a tonnage to buy at a market they are nowhere near.
     */
    @Test
    void theShuttleRunShowsWhatIsHeldOverWhatIsNeeded() {
        HudObjective objective = shuttleCard(wittHub(), Map.of(), Map.of("steel", 1600)).orElseThrow();

        assertEquals("1,600/2,274 T", valueOf(objective, "STEEL"),
                "what is held over what the site still wants - the panel's own REQUIRED figure");
    }

    /**
     * A good nothing has been bought of yet has no first half to show, so it stands on the requirement alone -
     * the same shape the rest of the card uses for "none of this aboard".
     */
    @Test
    void aGoodNotStartedShowsTheBareRequirement() {
        HudObjective objective = shuttleCard(wittHub(), Map.of(), Map.of("steel", 1600)).orElseThrow();

        assertEquals("7,041 T", valueOf(objective, "TITANIUM"));
    }

    @Test
    void theShuttleRunListsTheRestByDescendingDeficit() {
        HudObjective objective = shuttleCard(wittHub(), Map.of(), Map.of("steel", 1600)).orElseThrow();

        assertEquals(List.of("PROGRESS", "STEEL", "TITANIUM", "WATER", "FRUIT AND VEGETABLES",
                        "WATER PURIFIERS", "OUTSTANDING"), labels(objective),
                "steel is under way so it leads; everything else follows it largest first");
    }

    /**
     * Cargo already complete on the carrier is the whole reason the commander is flying to the depot. A list
     * of "what is left to buy" would drop it on the grounds that there is nothing left to buy.
     */
    @Test
    void aGoodFullyAboardTheCarrierStaysOnTheDeliveryList() {
        HudObjective objective = shuttleCard(wittHub(), Map.of(), Map.of("steel", 2274)).orElseThrow();

        assertEquals("2,274/2,274 T", valueOf(objective, "STEEL"), "bought in full, and still to be delivered");
        assertEquals(HudRow.State.GOOD, rowOf(objective, "STEEL").state());
    }

    /**
     * In flight towards a market with no carrier on the job, the next purchase is exactly what the commander
     * is about to act on, so the trip allocation stays.
     */
    @Test
    void noCarrierMeansTheTripAllocationSurvivesAwayFromAMarket() {
        HudObjective objective = card(site(0.67271, Instant.now().toString()), wittHub(), Map.of(), 880)
                .orElseThrow();

        assertEquals("880 T", valueOf(objective, "TITANIUM"), "one hold of the largest shortfall");
    }

    /**
     * The Witt Hub shuttle run, from the live case: steel is part delivered and the rest of it is sitting on
     * the carrier, so steel's remaining 3,154 has fallen behind titanium's 7,041. The commander is mid-job on
     * steel, and a card that switches them to titanium is telling them to start a second front.
     */
    @Test
    void aGoodAlreadyStartedLeadsTheLargerDeficit() {
        List<Requirement> wittHub = List.of(
                line("titanium", 7921, 880), line("aluminium", 4177, 0), line("steel", 8434, 5280));

        HudObjective objective = shuttleCard(wittHub, Map.of(), Map.of("steel", 1600)).orElseThrow();

        assertEquals("STEEL", labels(objective).get(1),
                "the steel on the carrier is bought and waiting - finish it before opening titanium");
    }

    /**
     * Tonnes in the ship's own hold say the same thing as tonnes on the carrier: this job is under way.
     */
    @Test
    void aGoodPartlyInTheHoldAlsoLeads() {
        List<Requirement> wittHub = List.of(
                line("titanium", 7921, 880), line("steel", 8434, 5280));

        HudObjective objective = card(site(0.67, Instant.now().toString()), wittHub, Map.of("steel", 400), 880)
                .orElseThrow();

        assertEquals("STEEL", labels(objective).get(1));
    }

    /**
     * Nothing started, so nothing to finish - the largest shortfall is the whole of the answer again.
     */
    @Test
    void withNothingStartedTheLargestDeficitStillLeads() {
        List<Requirement> wittHub = List.of(
                line("titanium", 7921, 880), line("aluminium", 4177, 0), line("steel", 8434, 5280));

        HudObjective objective = shuttleCard(wittHub, Map.of(), Map.of()).orElseThrow();

        assertEquals("TITANIUM", labels(objective).get(1));
    }

    /**
     * Two jobs under way rank between themselves the way everything else does, and both still lead the good
     * nobody has touched - even when that one is short by far the most.
     */
    @Test
    void twoStartedGoodsKeepDeficitOrderBetweenThem() {
        List<Requirement> tail = List.of(
                line("titanium", 7921, 0), line("aluminium", 4577, 4177), line("steel", 8434, 8134));

        HudObjective objective = shuttleCard(tail, Map.of(),
                Map.of("steel", 1600, "aluminium", 200)).orElseThrow();

        assertEquals(List.of("ALUMINIUM", "STEEL", "TITANIUM"), labels(objective).subList(1, 4),
                "aluminium is 400 short against steel's 300, and titanium's 7,921 waits behind both");
    }

    /**
     * Witt Hub read from The Chocolate Factory, which is where this came from: a Panther Mk II against a
     * build every one of whose big lines outsizes its hold. The trip is 880 tonnes of steel and always will
     * be, so a card that stops there names one good at a market selling three of the four the site wants.
     */
    @Test
    void whatElseThisMarketSellsFollowsTheTrip() {
        List<Requirement> wittHub = List.of(
                line("steel", 8434, 0), line("titanium", 7921, 0), line("aluminium", 4177, 0),
                line("water", 1609, 0), line("ceramiccomposites", 1207, 0));

        HudObjective objective = card(site(0.558048, Instant.now().toString()), wittHub, Map.of(), 880,
                Set.of("steel", "titanium", "aluminium")).orElseThrow();

        assertEquals("880 T", valueOf(objective, "STEEL"), "the trip is still one hold of the leading good");
        assertEquals(List.of("PROGRESS", "STEEL", "ALSO HERE", "TITANIUM", "ALUMINIUM", "OUTSTANDING"),
                labels(objective), "and what else is on these shelves follows it, under its own heading");
        assertEquals("7,921 T", valueOf(objective, "TITANIUM"),
                "at its own outstanding shortfall - it is a shopping note, not a second trip");
        assertEquals(SHOP.toUpperCase(), valueOf(objective, "ALSO HERE"), "named, so the pad is not in doubt");
    }

    /**
     * The heading is the whole distinction between "buy this now" and "this is also on the shelves", so a
     * card that never crosses into the second group must not draw it.
     */
    @Test
    void aTripThatCoversTheShelvesDrawsNoAlsoHereHeading() {
        List<Requirement> small = List.of(line("steel", 200, 0), line("titanium", 150, 0));

        HudObjective objective = card(site(0.5, Instant.now().toString()), small, Map.of(), HOLD,
                Set.of("steel", "titanium")).orElseThrow();

        assertEquals(List.of("PROGRESS", "STEEL", "TITANIUM", "OUTSTANDING"), labels(objective));
        assertEquals("200 T", valueOf(objective, "STEEL"));
        assertEquals("150 T", valueOf(objective, "TITANIUM"), "both fit, so both are this trip");
    }

    /**
     * Goods the port cannot supply are no use to a commander standing at its commodity screen - the trip
     * lists them because the next port is where they get bought, but the shopping note must not.
     */
    @Test
    void onlyGoodsThisMarketSellsFollowTheTrip() {
        List<Requirement> wittHub = List.of(
                line("steel", 8434, 0), line("water", 1609, 0), line("titanium", 7921, 0));

        HudObjective objective = card(site(0.558048, Instant.now().toString()), wittHub, Map.of(), 880,
                Set.of("steel", "titanium")).orElseThrow();

        assertEquals(List.of("PROGRESS", "STEEL", "ALSO HERE", "TITANIUM", "OUTSTANDING"), labels(objective));
        assertFalse(labels(objective).contains("WATER"), "not on these shelves, so not a note about them");
    }

    /**
     * Knowing nothing about the shelves, there is nothing to say about them - and the trip is still the
     * trip.
     */
    @Test
    void anUnknownMarketAddsNoShoppingNote() {
        List<Requirement> wittHub = List.of(line("steel", 8434, 0), line("titanium", 7921, 0));

        HudObjective objective = card(site(0.558048, Instant.now().toString()), wittHub, Map.of(), 880,
                Set.of()).orElseThrow();

        assertEquals(List.of("PROGRESS", "STEEL", "OUTSTANDING"), labels(objective));
    }

    /**
     * In flight, or at a market whose screen was never opened, we know nothing about what is on sale -
     * and ordering the card on a guess is worse than not ordering it.
     */
    @Test
    void knowingNothingAboutThePortLeavesTheOrderOnShortfall() {
        List<Requirement> tail = List.of(
                line("ceramiccomposites", 85, 0),
                line("superconductors", 60, 0));

        HudObjective objective = card(site(0.88, Instant.now().toString()), tail, Map.of(), HOLD, Set.of())
                .orElseThrow();

        assertEquals("CERAMIC COMPOSITES", labels(objective).get(1));
    }

    @Test
    void aHoldWithNothingForThisSiteDrawsNoInHoldRow() {
        HudObjective objective = card(site(0.026187, Instant.now().toString()), divisGateway(), Map.of())
                .orElseThrow();

        assertFalse(labels(objective).contains("IN HOLD"));
    }

    /**
     * Other commanders haul to the same depot, so tonnages read an hour ago are a claim about the past.
     * Saying so on the card is cheaper than sending the commander to buy something already delivered.
     */
    @Test
    void anOldManifestSaysSoOnTheCard() {
        String anHourAndAHalfAgo = Instant.now().minus(90, ChronoUnit.MINUTES).toString();

        HudObjective objective = card(site(0.026187, anHourAndAHalfAgo), divisGateway(), Map.of()).orElseThrow();

        assertEquals("LAST VISIT", valueOf(objective, "AS OF"));
        assertEquals(HudRow.State.WARN, rowOf(objective, "AS OF").state());
    }

    @Test
    void aFreshManifestCarriesNoCaveat() {
        HudObjective objective = card(site(0.026187, Instant.now().toString()), divisGateway(), Map.of())
                .orElseThrow();

        assertFalse(labels(objective).contains("AS OF"));
    }

    @Test
    void aFinishedBuildIsNotAnObjective() {
        Site finished = site(1.0, Instant.now().toString());
        finished.setComplete(true);

        assertTrue(card(finished, divisGateway(), Map.of()).isEmpty());
    }

    @Test
    void aFailedBuildIsNotAnObjectiveEither() {
        Site failed = site(0.4, Instant.now().toString());
        failed.setFailed(true);

        assertTrue(card(failed, divisGateway(), Map.of()).isEmpty());
    }

    /**
     * At this point the answer is "fly back and unload", which the plotted-route card already says.
     */
    @Test
    void aHoldThatCoversEverythingOutstandingDrawsNoCard() {
        assertTrue(card(site(0.9, Instant.now().toString()),
                List.of(line("steel", 2542, 2500)),
                Map.of("steel", 42)).isEmpty());
    }

    @Test
    void aManifestWithNothingLeftDrawsNoCard() {
        assertTrue(card(site(1.0, Instant.now().toString()),
                List.of(line("steel", 2542, 2542)), Map.of()).isEmpty());
    }

    @Test
    void neverHavingVisitedASiteDrawsNoCard() {
        assertTrue(card(null, List.of(), Map.of()).isEmpty());
    }

    /**
     * A restart on the pad leaves the manifest but not the {@code Docked} that named the place. The
     * manifest is the point of the card, so it is still drawn.
     */
    @Test
    void aSiteWithNoNameStillDrawsItsManifest() {
        Site nameless = site(0.026187, Instant.now().toString());
        nameless.setStationName(null);

        HudObjective objective = card(nameless, divisGateway(), Map.of()).orElseThrow();

        assertNull(objective.subtitle());
        assertEquals("640 T", valueOf(objective, "STEEL"));
    }

    /**
     * Filling the carrier at a market hundreds of light years from the build. The card stops being a hold's
     * worth of loading order and becomes the shelf in front of the commander: what is sold here that the
     * build wants, measured against the stockpile.
     */
    @Test
    void fillingTheCarrierTurnsTheCardIntoAShoppingList() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("steel", "titanium"),
                Map.of("steel", 400)).orElseThrow();

        assertEquals("400/2.542 T", valueOf(objective, "STEEL").replace(',', '.'),
                "what is stashed against what the build still wants");
        assertEquals(HudRow.State.GOOD, rowOf(objective, "STEEL").state());
        assertEquals("1.525 T", valueOf(objective, "TITANIUM").replace(',', '.'),
                "nothing of it aboard yet, so there is no ratio to draw");
        assertEquals(HudRow.State.NORMAL, rowOf(objective, "TITANIUM").state());
    }

    /**
     * Away from the carrier the unstocked goods stay on the card, because they will be bought at the next
     * port on the same run. Parked and stocking up there IS no next port - this shop is these shelves.
     */
    @Test
    void aShoppingListNamesOnlyWhatThisMarketSells() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("titanium"), Map.of()).orElseThrow();

        assertTrue(labels(objective).contains("TITANIUM"));
        assertFalse(labels(objective).contains("STEEL"), "steel is not on these shelves");
    }

    /**
     * The build's own requirement fixes the order, so the list reads the same before and after a hold-full
     * is bought - see {@link #buyingPartOfAGoodDoesNotPushItOffTheList}.
     */
    @Test
    void theBiggestRequirementLeads() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("steel", "titanium"),
                Map.of("steel", 2400)).orElseThrow();

        assertEquals("STEEL", labels(objective).get(2), "2.542 tonnes wanted against titanium's 1.525");
        assertEquals("TITANIUM", labels(objective).get(3));
    }

    /**
     * A market with nothing the build wants is not a shopping trip worth drawing, so the card goes back to
     * saying what the ship should be carrying.
     */
    @Test
    void aMarketSellingNothingTheBuildWantsFallsBackToTheLoadingOrder() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("gold", "silver"), Map.of())
                .orElseThrow();

        assertEquals("640 T", valueOf(objective, "STEEL"), "a hold's worth again, not the whole requirement");
    }

    /**
     * Tonnes in the hold are as bought as tonnes on the carrier, and both are going to the same depot, so
     * the row counts them as one figure rather than making the commander add them up.
     */
    @Test
    void whatIsInTheShipsHoldCountsAsBoughtToo() {
        HudObjective objective = shoppingCard(divisGateway(), Map.of("steel", 120),
                Set.of("steel"), Map.of("steel", 400)).orElseThrow();

        assertEquals("520/2.542 T", valueOf(objective, "STEEL").replace(',', '.'));
    }

    /**
     * The corner case the position rule got wrong: the carrier is elsewhere, but its steel is bought, and
     * the commander standing at a market that sells steel must not buy the whole requirement again.
     */
    @Test
    void aStockpileFarFromHereStillStopsTheCommanderBuyingItTwice() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("steel", "titanium"),
                Map.of("steel", 2000)).orElseThrow();

        assertEquals("2.000/2.542 T", valueOf(objective, "STEEL").replace(',', '.'));
    }

    /**
     * A market that sells Steel usually sells Copper, Aluminium and Titanium too, so the shopping list runs
     * to whatever the card can hold rather than the loading order's handful - and it is ordered by the
     * build's own requirement, which does not move while the commander shops.
     */
    @Test
    void theShoppingListRunsToWhatTheCardCanHold() {
        HudObjective objective = shoppingCard(wideBuild(),
                Set.of("steel", "titanium", "copper", "aluminium", "polymers"),
                Map.of("steel", 2500, "titanium", 1500)).orElseThrow();

        assertEquals(List.of("PROGRESS", "STATION", "STEEL", "TITANIUM", "COPPER", "ALUMINIUM", "POLYMERS",
                "OUTSTANDING"), labels(objective), "all five goods still short, under the shop's name");
        assertEquals("2.500/2.542 T", valueOf(objective, "STEEL").replace(',', '.'));
    }

    /**
     * The renderer keeps eight rows and drops the rest where it parses them - and the totals are written
     * under the goods, so an over-long list would cost the commander OUTSTANDING rather than a commodity.
     */
    @Test
    void theListNeverCrowdsTheTotalsOffTheCard() {
        List<Requirement> broadBuild = List.of(
                line("steel", 2542, 0), line("titanium", 1525, 0), line("copper", 900, 0),
                line("aluminium", 800, 0), line("polymers", 700, 0), line("cmmcomposite", 600, 0),
                line("liquidoxygen", 500, 0), line("water", 400, 0));

        HudObjective objective = new ConstructionSiteObjectiveSource(
                () -> site(0.026187, Instant.now().minus(90, ChronoUnit.MINUTES).toString()),
                marketId -> broadBuild, Map::of, () -> HOLD,
                () -> shop(Set.of("steel", "titanium", "copper", "aluminium", "polymers", "cmmcomposite",
                        "liquidoxygen", "water")),
                (ignored, alsoIgnored) -> Optional.of(new Stash("GHY-L8X", Map.of()))).currentObjective()
                .orElseThrow();

        assertEquals(8, objective.rows().size(), "the renderer's own limit");
        assertEquals(List.of("PROGRESS", "STATION", "STEEL", "TITANIUM", "COPPER", "ALUMINIUM",
                        "OUTSTANDING", "AS OF"), labels(objective),
                "the caveat, the shop's name and the total keep their rows; the list gives way at four goods");
        assertFalse(labels(objective).contains("CMM COMPOSITE"), "the four smallest requirements wait");
    }

    /**
     * Measured at Papin's Inheritance: buying titanium made CMM Composite the deeper deficit, and titanium -
     * the thing the commander was standing there buying - left the card. A checklist worked down over
     * several holds cannot reshuffle itself under the commander's hands.
     */
    @Test
    void buyingPartOfAGoodDoesNotPushItOffTheList() {
        Set<String> shelves = Set.of("steel", "titanium", "copper", "aluminium", "polymers");

        List<String> beforeBuying = labels(shoppingCard(wideBuild(), shelves, Map.of()).orElseThrow());
        List<String> afterBuying = labels(shoppingCard(wideBuild(), shelves,
                Map.of("titanium", 1400)).orElseThrow());

        assertEquals(beforeBuying, afterBuying, "125 tonnes short is still shopping: same rows, same places");
        assertEquals("1.400/1.525 T", valueOf(shoppingCard(wideBuild(), shelves, Map.of("titanium", 1400))
                .orElseThrow(), "TITANIUM").replace(',', '.'));
    }

    /**
     * Measured on the carrier at Lone Wolf: Polymers at 497/497 and Insulating Membrane at 311/311 were
     * still on the card, taking rows from goods the commander was there to buy. A good in hand is not
     * shopping.
     */
    @Test
    void aFullyBoughtGoodLeavesTheList() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("steel", "titanium"),
                Map.of("steel", 2542)).orElseThrow();

        assertFalse(labels(objective).contains("STEEL"), "497/497 is not something to buy");
        assertEquals("TITANIUM", labels(objective).get(2), "straight under the shop's name");
    }

    /**
     * With this system's shelves bought out, the card looks ahead: what the build still wants that cannot be
     * bought here. The commander then chooses between flying the stockpile in and moving it to another
     * market - and hears as much from {@code ConstructionShoppingAnnouncer} at the same moment.
     */
    @Test
    void aShopWithNothingLeftToBuyShowsWhatToAcquireNext() {
        List<Requirement> withATail = List.of(
                line("steel", 2542, 0), line("titanium", 1525, 0), line("cmmcomposite", 880, 0));

        HudObjective objective = shoppingCard(withATail, Set.of("steel", "titanium"),
                Map.of("steel", 2542, "titanium", 1525)).orElseThrow();

        assertEquals(List.of("PROGRESS", "SOURCE", "CMM COMPOSITE", "OUTSTANDING"), labels(objective),
                "the one thing left to acquire, and none of what is already aboard");
        assertEquals("880 T", valueOf(objective, "CMM COMPOSITE"));
        assertEquals("ELSEWHERE", valueOf(objective, "SOURCE"),
                "the emptied pad is not named over goods it does not sell");
    }

    /**
     * And with nothing left to acquire anywhere, the finished goods stand: everything is bought and the
     * answer is to fly it in. Still better than reverting to the build's largest shortfall at a market that
     * does not sell it.
     */
    @Test
    void owningTheWholeBuildLeavesTheFinishedGoodsOnTheCard() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("steel", "titanium"),
                Map.of("steel", 2542, "titanium", 1525)).orElseThrow();

        assertEquals("2.542/2.542 T", valueOf(objective, "STEEL").replace(',', '.'));
        assertEquals("1.525/1.525 T", valueOf(objective, "TITANIUM").replace(',', '.'));
        assertEquals(HudRow.State.GOOD, rowOf(objective, "STEEL").state());
    }

    /**
     * The hold counts the same as the carrier, so a good the ship is already carrying enough of is bought
     * and gone from the list too.
     */
    @Test
    void aGoodTheHoldAlreadyCoversIsBoughtToo() {
        HudObjective objective = shoppingCard(divisGateway(), Map.of("titanium", 1525),
                Set.of("steel", "titanium"), Map.of()).orElseThrow();

        assertFalse(labels(objective).contains("TITANIUM"));
        assertEquals("2.542 T", valueOf(objective, "STEEL").replace(',', '.'));
    }

    /**
     * Everything under the commodity rows is unchanged - the trip has a different shape, the build does not.
     */
    @Test
    void theBuildsOwnFiguresAreTheSameOnAShoppingTrip() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("steel"), Map.of("steel", 400))
                .orElseThrow();

        assertEquals(3, rowOf(objective, "PROGRESS").current());
        assertEquals("4.067 T", valueOf(objective, "OUTSTANDING").replace(',', '.'),
                "the whole build's shortfall, which the carrier's stash does not change");
        assertEquals("ORBITAL CONSTRUCTION SITE: DIVIS GATEWAY", objective.subtitle());
    }

    /**
     * The list changes the moment the commander jumps into a system whose market has what the build wants -
     * which says nothing until they know which pad it is. Named under the progress bar, above the goods it
     * describes.
     */
    @Test
    void theShoppingListSaysWhichMarketItIsReadFrom() {
        HudObjective objective = shoppingCard(divisGateway(), Set.of("steel"), Map.of()).orElseThrow();

        assertEquals("STATION", labels(objective).get(1), "under the progress bar, above the goods");
        assertEquals("PAPIN'S INHERITANCE", valueOf(objective, "STATION"));
    }

    /**
     * A loading order is about the ship rather than any one market, so there is no pad to name and the row
     * would only cost a commodity its place.
     */
    @Test
    void aLoadingOrderNamesNoStation() {
        HudObjective objective = card(site(0.026187, Instant.now().toString()), divisGateway(), Map.of())
                .orElseThrow();

        assertFalse(labels(objective).contains("STATION"));
    }
}
