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
}
