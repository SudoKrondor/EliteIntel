package elite.intel.gameapi.colonisation;

import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.gameapi.carrier.OwnCarrierHold.Held;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finding the carrier a build is being stockpiled on.
 * <p>
 * The build is Divis Gateway's, out in Hyades Sector NR-V b2-2, and it still wants steel and titanium. The
 * commander is buying for it in Sirius, with the lorry - GHY-L8X - somewhere.
 */
class CarrierStockpileTest {

    private static final String SITE_SYSTEM = "Hyades Sector NR-V b2-2";
    private static final String MARKET_SYSTEM = "Sirius";
    private static final String CARRIER = "GHY-L8X";
    private static final String SQUADRON = "XYZ-99Z";
    private static final Set<String> WANTED = Set.of("steel", "titanium");

    private static Site site() {
        Site site = new Site();
        site.setMarketId(3967232514L);
        site.setStationName("Orbital Construction Site: Divis Gateway");
        site.setStarSystem(SITE_SYSTEM);
        return site;
    }

    private static Held carrier(String callSign, String starSystem, Map<String, Integer> stock) {
        return new Held(callSign, starSystem, Instant.now(), stock);
    }

    private static Optional<CarrierStockpile.Stash> stash(Held... carriers) {
        return CarrierStockpile.forBuild(site(), WANTED, MARKET_SYSTEM, List.of(carriers));
    }

    @Test
    void aCarrierParkedAtTheMarketIsBeingFilledForTheBuild() {
        CarrierStockpile.Stash stash = stash(
                carrier(CARRIER, MARKET_SYSTEM, Map.of("steel", 400))).orElseThrow();

        assertEquals(CARRIER, stash.callSign());
        assertEquals(400, stash.stockOf("steel"));
        assertEquals(0, stash.stockOf("titanium"));
    }

    /**
     * The moment the whole list is still to buy, and the one an "only carriers with cargo" lookup would
     * have thrown away.
     */
    @Test
    void aCarrierThatHasJustParkedIsStillAStockpile() {
        CarrierStockpile.Stash stash = stash(carrier(CARRIER, MARKET_SYSTEM, Map.of())).orElseThrow();

        assertTrue(stash.isEmpty());
        assertEquals(0, stash.stockOf("steel"));
    }

    /**
     * The corner case that made the ledger the rule rather than the position: the carrier is a hundred jumps
     * away, but the 2,000 tonnes of steel on it are still bought, and the commander must not buy them twice.
     */
    @Test
    void aCarrierAnywhereCountsOnceItIsCarryingTheBuildsCargo() {
        CarrierStockpile.Stash stash = stash(
                carrier(CARRIER, "Sol", Map.of("steel", 2000))).orElseThrow();

        assertEquals(2000, stash.stockOf("steel"));
    }

    /**
     * Sitting at the depot with the build's cargo, it is being emptied rather than filled - but the tonnes
     * are still bought, so they still count. Only the empty carrier at the site says nothing.
     */
    @Test
    void anEmptyCarrierAtTheBuildIsNotAStockpile() {
        assertTrue(CarrierStockpile.forBuild(site(), WANTED, SITE_SYSTEM,
                List.of(carrier(CARRIER, SITE_SYSTEM, Map.of()))).isEmpty());
    }

    @Test
    void anEmptyCarrierSomewhereElseEntirelyIsNotAStockpileEither() {
        assertTrue(stash(carrier(CARRIER, "Sol", Map.of())).isEmpty());
    }

    /**
     * A hold full of tritium and gold says nothing about whether this carrier is working this build.
     */
    @Test
    void cargoTheBuildDoesNotWantDoesNotMakeItAStockpile() {
        assertTrue(stash(carrier(CARRIER, "Sol", Map.of("tritium", 900, "gold", 40))).isEmpty());
    }

    /**
     * The shuttle spends its time between the market and the carrier, so the answer cannot depend on which
     * pad the ship is on - see {@code ShoppingShelves} for which market counts as the shop.
     */
    @Test
    void standingOnTheCarrierChangesNothing() {
        assertEquals(400, stash(carrier(CARRIER, MARKET_SYSTEM, Map.of("steel", 400)))
                .orElseThrow().stockOf("steel"));
    }

    @Test
    void withNoCarrierAtAllThereIsNoStockpile() {
        assertTrue(stash().isEmpty());
    }

    @Test
    void aBuildThatWantsNothingHasNothingToStockpileFor() {
        assertTrue(CarrierStockpile.forBuild(site(), Set.of(), MARKET_SYSTEM,
                List.of(carrier(CARRIER, MARKET_SYSTEM, Map.of("steel", 400)))).isEmpty());
    }

    /**
     * One is the lorry, the other is packed for squadron work elsewhere. The tonnes are read off the one
     * doing the job, never summed: a total that sits in no single hold cannot be delivered as one.
     */
    @Test
    void theCarrierParkedWhereWeAreShoppingIsTheOneWeRead() {
        CarrierStockpile.Stash stash = stash(
                carrier(SQUADRON, "Sol", Map.of("steel", 9000)),
                carrier(CARRIER, MARKET_SYSTEM, Map.of("steel", 400))).orElseThrow();

        assertEquals(CARRIER, stash.callSign());
        assertEquals(400, stash.stockOf("steel"));
    }

    /**
     * With neither parked here, the one actually hauling this build's cargo is the one that counts.
     */
    @Test
    void otherwiseTheCarrierCarryingMoreOfTheBuildWins() {
        CarrierStockpile.Stash stash = stash(
                carrier(SQUADRON, "Sol", Map.of("steel", 60)),
                carrier(CARRIER, "Achenar", Map.of("steel", 2000))).orElseThrow();

        assertEquals(CARRIER, stash.callSign());
    }

    /**
     * A site we never learned the system of cannot be told apart from the one we are standing in - but a
     * carrier already loaded for it needs no such comparison.
     */
    @Test
    void aSiteOfUnknownSystemStillReadsTheLoadedCarrier() {
        Site nowhere = site();
        nowhere.setStarSystem(null);

        assertTrue(CarrierStockpile.forBuild(nowhere, WANTED, MARKET_SYSTEM,
                List.of(carrier(CARRIER, MARKET_SYSTEM, Map.of()))).isEmpty());
        assertEquals(400, CarrierStockpile.forBuild(nowhere, WANTED, MARKET_SYSTEM,
                List.of(carrier(CARRIER, MARKET_SYSTEM, Map.of("steel", 400)))).orElseThrow().stockOf("steel"));
    }
}
