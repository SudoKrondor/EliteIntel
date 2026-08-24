package elite.intel.ui.overlay;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
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
                () -> capacity, () -> stockedHere).currentObjective();
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
}
