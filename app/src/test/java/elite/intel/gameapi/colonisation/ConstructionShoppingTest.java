package elite.intel.gameapi.colonisation;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ConstructionCargo.Outstanding;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The manifest read as a shopping list. The screen and the voice both work off this, so "bought out" has to
 * mean one thing.
 * <p>
 * The build is Nomen Relay's; the shop is Papin's Inheritance, which sells four of the goods it wants.
 */
class ConstructionShoppingTest {

    private static final Set<String> SHELVES = Set.of("titanium", "steel", "liquidoxygen", "polymers");

    private static Requirement line(String symbol, int required, int provided) {
        Requirement requirement = new Requirement();
        requirement.setSymbol(symbol);
        requirement.setGameName(symbol);
        requirement.setRequiredAmount(required);
        requirement.setProvidedAmount(provided);
        return requirement;
    }

    /**
     * Nomen Relay at 21 per cent, with CMM Composite - which Papin's does not sell - still outstanding.
     */
    private static List<Outstanding> nomenRelay(Map<String, Integer> hold) {
        return ConstructionCargo.outstanding(List.of(
                line("titanium", 3963, 0),
                line("steel", 2810, 0),
                line("liquidoxygen", 1553, 0),
                line("polymers", 497, 0),
                line("cmmcomposite", 880, 0)), hold);
    }

    private static Stash carrier(Map<String, Integer> stock) {
        return new Stash("LONE WOLF", stock);
    }

    @Test
    void whatThisMarketSellsComesBackLargestRequirementFirst() {
        List<ConstructionShopping.Line> sold = ConstructionShopping.soldHere(
                nomenRelay(Map.of()), SHELVES, carrier(Map.of()));

        assertEquals(List.of("titanium", "steel", "liquidoxygen", "polymers"),
                sold.stream().map(ConstructionShopping.Line::symbol).toList());
        assertFalse(sold.stream().anyMatch(line -> "cmmcomposite".equals(line.symbol())),
                "not on these shelves, so not part of this shop");
    }

    /**
     * Measured at Lone Wolf: the carrier's own hold is where most of a stockpile sits, and the ship's is the
     * hold-full on its way to join it. Both are bought.
     */
    @Test
    void theCarrierAndTheHoldCountAsOnePossession() {
        List<ConstructionShopping.Line> sold = ConstructionShopping.soldHere(
                nomenRelay(Map.of("polymers", 97)), SHELVES, carrier(Map.of("polymers", 400)));

        ConstructionShopping.Line polymers = sold.stream()
                .filter(line -> "polymers".equals(line.symbol())).findFirst().orElseThrow();
        assertEquals(497, polymers.owned());
        assertEquals(497, polymers.needed());
        assertTrue(polymers.isCovered());
    }

    @Test
    void aShopIsBoughtOutOnlyWhenEveryGoodOnItsShelvesIsCovered() {
        Map<String, Integer> nearlyThere = Map.of("titanium", 3963, "steel", 2810, "liquidoxygen", 1553,
                "polymers", 496);

        assertFalse(ConstructionShopping.isBoughtOut(
                        ConstructionShopping.soldHere(nomenRelay(Map.of()), SHELVES, carrier(nearlyThere))),
                "one tonne short is still shopping");

        Map<String, Integer> done = Map.of("titanium", 3963, "steel", 2810, "liquidoxygen", 1553,
                "polymers", 497);
        assertTrue(ConstructionShopping.isBoughtOut(
                ConstructionShopping.soldHere(nomenRelay(Map.of()), SHELVES, carrier(done))));
    }

    /**
     * Flying past a market that never had anything for the build is not an achievement, and must not be
     * announced as one.
     */
    @Test
    void aMarketThatSellsNothingTheBuildWantsIsNotBoughtOut() {
        assertFalse(ConstructionShopping.isBoughtOut(
                ConstructionShopping.soldHere(nomenRelay(Map.of()), Set.of("gold", "silver"), carrier(Map.of()))));
    }

    /**
     * What is left once the shop is finished - the answer to "what now", and what the card shows.
     */
    @Test
    void whatIsLeftToAcquireIgnoresTheShelvesEntirely() {
        Map<String, Integer> shopDone = Map.of("titanium", 3963, "steel", 2810, "liquidoxygen", 1553,
                "polymers", 497);

        List<ConstructionShopping.Line> next =
                ConstructionShopping.stillToAcquire(nomenRelay(Map.of()), carrier(shopDone));

        assertEquals(List.of("cmmcomposite"), next.stream()
                .map(ConstructionShopping.Line::symbol).toList());
        assertEquals(880, next.getFirst().needed());
    }

    @Test
    void owningTheWholeBuildLeavesNothingToAcquire() {
        Map<String, Integer> everything = Map.of("titanium", 3963, "steel", 2810, "liquidoxygen", 1553,
                "polymers", 497, "cmmcomposite", 880);

        assertTrue(ConstructionShopping.stillToAcquire(nomenRelay(Map.of()), carrier(everything)).isEmpty());
    }

    /**
     * Witt Hub wanted 1,858 more tonnes of Aluminium, 1,760 of them were already on the carrier, and the
     * commander was told there was no need to buy any while standing at the market that sold the other 98.
     * A hold-full is never a statement about the build - what is left to buy is a question about the whole
     * stock, and this is the figure the search and the card both spend.
     */
    @Test
    void whatIsLeftToBuyIsMeasuredAgainstTheWholeStockpile() {
        List<Outstanding> wittHub = ConstructionCargo.outstanding(
                List.of(line("aluminium", 1858, 0)), Map.of());

        ConstructionShopping.Line line =
                ConstructionShopping.stillToAcquire(wittHub, carrier(Map.of("aluminium", 1760))).getFirst();

        assertEquals(1858, line.needed());
        assertEquals(1760, line.owned());
        assertEquals(98, line.stillToBuy());
        assertFalse(line.isCovered());
    }

    /**
     * Tonnes in the hold and tonnes on the carrier are both paid for and both bound for the depot, so they
     * come off the same figure.
     */
    @Test
    void theHoldAndTheCarrierCountAgainstTheSameLine() {
        List<Outstanding> wittHub = ConstructionCargo.outstanding(
                List.of(line("aluminium", 1858, 0)), Map.of("aluminium", 98));

        assertEquals(0, ConstructionShopping.stillToAcquire(wittHub, carrier(Map.of("aluminium", 1760))).size(),
                "nothing left to buy once both are counted");
        assertEquals(1858, ConstructionShopping.toDeliver(wittHub, carrier(Map.of("aluminium", 1760)))
                .getFirst().owned(), "and all of it is already ours to deliver");
    }

    /**
     * Over-buying a good finishes it; it never turns into a negative errand that could out-rank a real one.
     */
    @Test
    void aGoodBoughtTwiceOverIsSimplyFinished() {
        List<Outstanding> wittHub = ConstructionCargo.outstanding(
                List.of(line("aluminium", 1858, 0)), Map.of());

        assertEquals(0, ConstructionShopping.toDeliver(wittHub, carrier(Map.of("aluminium", 9000)))
                .getFirst().stillToBuy());
    }
}
