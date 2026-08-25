package elite.intel.gameapi.colonisation;

import elite.intel.db.managers.StationMarketsManager.MarketSnapshot;
import elite.intel.gameapi.colonisation.ShoppingShelves.Pad;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeping the shop on screen across a shuttle run.
 * <p>
 * Papin's Inheritance in Sirius sells what the build wants; the carrier GHY-L8X is parked in the same
 * system. The commander buys, lifts off, unloads onto the carrier and comes back, and the card has to say
 * the same thing throughout.
 */
class ShoppingShelvesTest {

    private static final String SYSTEM = "Sirius";
    private static final Pad STATION = new Pad(3223343616L, "Papin's Inheritance");
    private static final Pad CARRIER = new Pad(3700000000L, "GHY-L8X");
    private static final Pad NOWHERE = new Pad(0, null);

    private static final MarketSnapshot STATION_SHELF = new MarketSnapshot(SYSTEM, STATION.stationName(),
            Instant.now(), Map.of("steel", 4000, "titanium", 3000));
    private static final MarketSnapshot CARRIER_SHELF = new MarketSnapshot(SYSTEM, CARRIER.stationName(),
            Instant.now(), Map.of("cmmcomposite", 880));

    private final AtomicReference<Pad> pad = new AtomicReference<>(NOWHERE);
    private final AtomicReference<String> system = new AtomicReference<>(SYSTEM);

    private final ShoppingShelves shelves = new ShoppingShelves(
            pad::get,
            here -> CARRIER.stationName().equals(here.stationName()),
            here -> {
                if (STATION.equals(here)) return Optional.of(STATION_SHELF);
                if (CARRIER.equals(here)) return Optional.of(CARRIER_SHELF);
                return Optional.empty();
            },
            system::get);

    @Test
    void standingInTheMarketReadsItsShelves() {
        pad.set(STATION);

        assertEquals(Set.of("steel", "titanium"), shelves.stocked());
    }

    /**
     * The bug this class exists for: lifting off used to lose the shop, and the card fell back to naming the
     * build's largest shortfall - a good sold nowhere in this system.
     */
    @Test
    void liftingOffKeepsTheShopWeWereJustIn() {
        pad.set(STATION);
        shelves.stocked();
        pad.set(NOWHERE);

        assertEquals(Set.of("steel", "titanium"), shelves.stocked(), "same system, same shop");
    }

    /**
     * The other half of the shuttle. Our own carrier's shelves are the stockpile, not a shop - a list built
     * from them would tell the commander to buy their own cargo.
     */
    @Test
    void dockingAtOurCarrierKeepsTheShopRatherThanReadingTheStockpile() {
        pad.set(STATION);
        shelves.stocked();
        pad.set(CARRIER);

        assertEquals(Set.of("steel", "titanium"), shelves.stocked());
    }

    @Test
    void ourCarrierIsNoShopEvenWithNothingRememberedYet() {
        pad.set(CARRIER);

        assertTrue(shelves.stocked().isEmpty());
    }

    /**
     * A jump away, the shop says nothing about what can be bought here.
     */
    @Test
    void leavingTheSystemLetsTheShopGo() {
        pad.set(STATION);
        shelves.stocked();
        pad.set(NOWHERE);
        system.set("Hyades Sector NR-V b2-2");

        assertTrue(shelves.stocked().isEmpty());
    }

    /**
     * And coming back to it picks it up again - it is the same market, and we have not been anywhere that
     * could have changed what we know about its shelves.
     */
    @Test
    void comingBackToTheSystemPicksTheShopUpAgain() {
        pad.set(STATION);
        shelves.stocked();
        pad.set(NOWHERE);
        system.set("Hyades Sector NR-V b2-2");
        shelves.stocked();
        system.set(SYSTEM);

        assertEquals(Set.of("steel", "titanium"), shelves.stocked());
    }

    @Test
    void aMarketWeHaveNeverOpenedIsNoShelfAtAll() {
        pad.set(new Pad(4200000000L, "Somewhere Else"));

        assertTrue(shelves.stocked().isEmpty());
    }

    /**
     * A second shop in the same system replaces the first: it is the one the commander is standing in.
     */
    @Test
    void theShopIsAlwaysTheLastOneWeStoodIn() {
        Pad other = new Pad(1234L, "Boldyr Dredging Installation");
        MarketSnapshot otherShelf = new MarketSnapshot(SYSTEM, other.stationName(), Instant.now(),
                Map.of("polymers", 500));
        ShoppingShelves twoShops = new ShoppingShelves(pad::get, here -> false,
                here -> Optional.of(STATION.equals(here) ? STATION_SHELF : otherShelf), system::get);

        pad.set(STATION);
        twoShops.stocked();
        pad.set(other);
        twoShops.stocked();
        pad.set(NOWHERE);

        assertEquals(Set.of("polymers"), twoShops.stocked());
    }
}
