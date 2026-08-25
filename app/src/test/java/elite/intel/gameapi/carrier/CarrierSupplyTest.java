package elite.intel.gameapi.carrier;

import elite.intel.gameapi.carrier.OwnCarrierHold.Held;
import elite.intel.gameapi.search.spansh.commodity.WantedCommodity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Buying steel the commander already owns, parked in the next orbit, is the wrong answer - and it is the
 * answer they got before this, because the construction search only ever looked outward.
 * <p>
 * The carrier is GHY-L8X, the commander's own, and the manifest is Divis Gateway's long tail.
 */
class CarrierSupplyTest {

    private static final String HERE = "Hyades Sector NR-V b2-2";
    private static final String ELSEWHERE = "Sterope";

    private static Held carrierAt(String starSystem, Map<String, Integer> stock) {
        return new Held("GHY-L8X", starSystem, Instant.now(), stock);
    }

    private static WantedCommodity want(String symbol, String commodity, int units) {
        return new WantedCommodity(symbol, commodity, units);
    }

    private static List<WantedCommodity> longTail() {
        return List.of(
                want("ceramiccomposites", "Ceramic Composites", 85),
                want("polymers", "Polymers", 170),
                want("copper", "Copper", 85));
    }

    @Test
    void theCarrierSuppliesWhatItHasOfTheList() {
        Held carrier = carrierAt(HERE, Map.of("ceramiccomposites", 900, "copper", 900));

        List<CarrierSupply.Line> loadable = CarrierSupply.loadable(carrier, longTail(), 512);

        assertEquals(2, loadable.size(), "polymers are not aboard and are not invented");
        assertEquals("Ceramic Composites", loadable.getFirst().commodity());
        assertEquals(85, loadable.getFirst().unitsToLoad());
        assertEquals(85, loadable.get(1).unitsToLoad());
    }

    @Test
    void neverMoreThanTheBuildStillWants() {
        Held carrier = carrierAt(HERE, Map.of("ceramiccomposites", 9000));

        assertEquals(85, CarrierSupply.loadable(carrier, longTail(), 512).getFirst().unitsToLoad());
    }

    @Test
    void neverMoreThanIsActuallyAboard() {
        Held carrier = carrierAt(HERE, Map.of("ceramiccomposites", 12));

        CarrierSupply.Line line = CarrierSupply.loadable(carrier, longTail(), 512).getFirst();
        assertEquals(12, line.unitsToLoad());
        assertEquals(12, line.aboard(), "and the commander is told how little that is");
    }

    @Test
    void neverMoreThanTheShipCanCarry() {
        Held carrier = carrierAt(HERE, Map.of("ceramiccomposites", 900, "polymers", 900, "copper", 900));

        List<CarrierSupply.Line> loadable = CarrierSupply.loadable(carrier, longTail(), 100);

        assertEquals(100, loadable.stream().mapToInt(CarrierSupply.Line::unitsToLoad).sum());
        assertEquals(85, loadable.getFirst().unitsToLoad(), "the anchor is filled first");
        assertEquals(15, loadable.get(1).unitsToLoad());
    }

    /**
     * The cargo is already bought and it is right there - no tonnage is too small to be worth collecting.
     */
    @Test
    void aCarrierInThisSystemIsAlwaysWorthUsing() {
        Held carrier = carrierAt(HERE, Map.of("ceramiccomposites", 3));

        List<CarrierSupply.Line> loadable = CarrierSupply.loadable(carrier, longTail(), 512);

        assertTrue(CarrierSupply.worthGoing(carrier, loadable, 512, HERE));
        assertTrue(CarrierSupply.isInSystem(carrier, HERE));
    }

    /**
     * A journey across the bubble to collect three tonnes is a worse answer than the market run it would
     * displace.
     */
    @Test
    void aCarrierElsewhereHoldingAlmostNothingIsNotWorthTheJump() {
        Held carrier = carrierAt(ELSEWHERE, Map.of("ceramiccomposites", 3));

        List<CarrierSupply.Line> loadable = CarrierSupply.loadable(carrier, longTail(), 512);

        assertFalse(CarrierSupply.worthGoing(carrier, loadable, 512, HERE));
    }

    @Test
    void aCarrierElsewhereHoldingARealLoadIsWorthTheJump() {
        Held carrier = carrierAt(ELSEWHERE, Map.of("ceramiccomposites", 85, "polymers", 170));

        List<CarrierSupply.Line> loadable = CarrierSupply.loadable(carrier, longTail(), 512);

        assertEquals(255, loadable.stream().mapToInt(CarrierSupply.Line::unitsToLoad).sum());
        assertTrue(CarrierSupply.worthGoing(carrier, loadable, 512, HERE),
                "half a hold of what the build needs, already paid for");
    }

    @Test
    void aCarrierWithNothingOnTheListSendsNobodyAnywhere() {
        Held carrier = carrierAt(HERE, Map.of("gold", 700, "tea", 300));

        List<CarrierSupply.Line> loadable = CarrierSupply.loadable(carrier, longTail(), 512);

        assertTrue(loadable.isEmpty());
        assertFalse(CarrierSupply.worthGoing(carrier, loadable, 512, HERE),
                "a hold full of gold is not construction cargo");
    }

    /**
     * A carrier's market lists every good it has ever carried, most of them at zero. Those are the residue
     * of what has already been hauled away, not stock.
     */
    @Test
    void aGoodListedAtZeroIsNotAboard() {
        Held carrier = carrierAt(HERE, Map.of("ceramiccomposites", 0));

        assertTrue(CarrierSupply.loadable(carrier, longTail(), 512).isEmpty());
    }

    /**
     * A commander can own one fleet carrier and one squadron carrier. The fleet carrier holding a few tonnes
     * of leftover trade goods must not mask the squadron carrier loaded with what the build actually needs.
     */
    @Test
    void theCarrierThatCanSupplyTheBuildWinsOverOneMerelyCarryingSomething() {
        Held fleet = new Held("GHY-L8X", ELSEWHERE, Instant.now(), Map.of("gold", 700));
        Held squadron = new Held("SQD-001", ELSEWHERE, Instant.now(),
                Map.of("ceramiccomposites", 85, "polymers", 170));

        CarrierSupply.Loaded best =
                CarrierSupply.best(List.of(fleet, squadron), longTail(), 512, HERE).orElseThrow();

        assertEquals("SQD-001", best.carrier().callSign());
        assertEquals(255, best.tonnes());
    }

    /**
     * Cargo already paid for and in the same system needs no journey at all, so it beats a bigger load
     * somewhere else.
     */
    @Test
    void aCarrierInThisSystemBeatsAFullerOneElsewhere() {
        Held here = new Held("GHY-L8X", HERE, Instant.now(), Map.of("ceramiccomposites", 20));
        Held far = new Held("SQD-001", ELSEWHERE, Instant.now(),
                Map.of("ceramiccomposites", 85, "polymers", 170));

        CarrierSupply.Loaded best =
                CarrierSupply.best(List.of(far, here), longTail(), 512, HERE).orElseThrow();

        assertEquals("GHY-L8X", best.carrier().callSign());
        assertTrue(best.here());
    }

    @Test
    void carriersHoldingNothingOnTheListYieldNoAnswer() {
        Held fleet = new Held("GHY-L8X", HERE, Instant.now(), Map.of("gold", 700));

        assertTrue(CarrierSupply.best(List.of(fleet), longTail(), 512, HERE).isEmpty());
        assertTrue(CarrierSupply.best(List.of(), longTail(), 512, HERE).isEmpty());
        assertTrue(CarrierSupply.best(null, longTail(), 512, HERE).isEmpty());
    }

    /**
     * The game reports a carrier's cargo exactly once, while the commander stands in its market, and never
     * mentions anything moved on or off afterwards. An old snapshot is a claim about the past, and flying
     * somewhere on it is how a commander arrives to find the cargo already hauled away.
     */
    @Test
    void anOldLookAtTheShelvesIsFlaggedAsOld() {
        Held stale = new Held("GHY-L8X", HERE,
                Instant.now().minus(CarrierSupply.SNAPSHOT_STALE_AFTER_HOURS + 1, ChronoUnit.HOURS),
                Map.of("ceramiccomposites", 85));

        assertTrue(CarrierSupply.snapshotIsStale(stale));
    }

    @Test
    void aLookFromThisSessionCarriesNoCaveat() {
        Held fresh = new Held("GHY-L8X", HERE, Instant.now(), Map.of("ceramiccomposites", 85));

        assertFalse(CarrierSupply.snapshotIsStale(fresh),
                "within a session the commander knows what they loaded");
    }

    @Test
    void aCarrierWeHaveNeverTimestampedIsNotCalledStale() {
        assertFalse(CarrierSupply.snapshotIsStale(
                new Held("GHY-L8X", HERE, null, Map.of("ceramiccomposites", 85))));
        assertFalse(CarrierSupply.snapshotIsStale(null));
    }

    @Test
    void noCarrierIsQuietRatherThanFatal() {
        assertTrue(CarrierSupply.loadable(null, longTail(), 512).isEmpty());
        assertFalse(CarrierSupply.worthGoing(null, List.of(), 512, HERE));
        assertFalse(CarrierSupply.isInSystem(null, HERE));
    }

    @Test
    void anUnknownCurrentSystemIsNotTakenForAMatch() {
        Held carrier = carrierAt(HERE, Map.of("ceramiccomposites", 85));

        assertFalse(CarrierSupply.isInSystem(carrier, null));
    }
}
