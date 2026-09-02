package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ConstructionCargo;
import elite.intel.gameapi.colonisation.ConstructionCargo.Outstanding;
import elite.intel.gameapi.colonisation.ConstructionShopping;
import elite.intel.gameapi.search.spansh.commodity.WantedCommodity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the colonisation search is actually sent shopping for.
 * <p>
 * The commander asked where to BUY construction materials while their own carrier held most of the build's
 * Aluminium. Before this, the tonnages spoken and searched for were measured against the ship's hold alone,
 * so the answer was a 1,858 tonne errand for 98 tonnes of cargo - and the carrier's shelves were read out
 * loud on top of it, which is the chatter that buried the figure they wanted.
 * <p>
 * The build is Witt Hub's, the carrier is GHY-L8X, and the market run is for whatever is left after it.
 */
class FindConstructionCommodityShoppingListTest {

    private static Requirement line(String symbol, int required, int provided) {
        Requirement requirement = new Requirement();
        requirement.setSymbol(symbol);
        requirement.setGameName(symbol);
        requirement.setRequiredAmount(required);
        requirement.setProvidedAmount(provided);
        return requirement;
    }

    private static List<Outstanding> wittHub(Map<String, Integer> hold) {
        return ConstructionCargo.outstanding(List.of(
                line("aluminium", 1858, 0),
                line("steel", 600, 0),
                line("titanium", 240, 0)), hold);
    }

    private static List<WantedCommodity> shoppingList(List<Outstanding> manifest, Stash stash) {
        return FindConstructionCommodityCommand.shoppingList(
                FindConstructionCommodityCommand.longPoleFirst(
                        ConstructionShopping.stillToAcquire(manifest, stash)));
    }

    /**
     * The whole point: a good already stockpiled is a good already bought, and the search must be sized to
     * what is missing rather than to what the manifest says.
     */
    @Test
    void theCarrierComesOffEveryTonnageTheSearchIsGiven() {
        List<WantedCommodity> wanted =
                shoppingList(wittHub(Map.of()), new Stash("GHY-L8X", Map.of("aluminium", 1760)));

        assertEquals(3, wanted.size());
        assertEquals(98, wanted.stream()
                .filter(want -> "aluminium".equals(want.symbol())).findFirst().orElseThrow().unitsWanted());
    }

    /**
     * The anchor names the good the trip is for and is what Spansh searches on, so it has to be the largest
     * thing LEFT to buy. A 98 tonne errand must not out-rank 600 tonnes nobody has bought yet just because
     * the manifest wants more of it in total.
     */
    @Test
    void theAnchorIsTheLargestThingLeftToBuyNotTheLargestRequirement() {
        List<WantedCommodity> wanted =
                shoppingList(wittHub(Map.of()), new Stash("GHY-L8X", Map.of("aluminium", 1760)));

        assertEquals(List.of("steel", "titanium", "aluminium"),
                wanted.stream().map(WantedCommodity::symbol).toList());
        assertEquals("Steel", wanted.getFirst().commodity(), "named as Spansh spells it");
        assertEquals(600, wanted.getFirst().unitsWanted());
    }

    /**
     * With no carrier in the answer the market run has to cover the whole list - the alternative is silently
     * shopping for less than the build wants.
     */
    @Test
    void noCarrierTakesNothingOffTheList() {
        List<WantedCommodity> wanted = shoppingList(wittHub(Map.of()), null);

        assertEquals(List.of("aluminium", "steel", "titanium"),
                wanted.stream().map(WantedCommodity::symbol).toList());
        assertEquals(1858, wanted.getFirst().unitsWanted());
    }

    /**
     * The hold is counted too, and against the same figure: cargo already aboard the ship is on its way to
     * the depot exactly as the carrier's is.
     */
    @Test
    void theHoldIsCountedAlongsideTheCarrier() {
        List<WantedCommodity> wanted = shoppingList(
                wittHub(Map.of("steel", 600)), new Stash("GHY-L8X", Map.of("aluminium", 1760)));

        assertEquals(List.of("titanium", "aluminium"),
                wanted.stream().map(WantedCommodity::symbol).toList(),
                "steel is aboard, so it is not shopping");
    }

    /**
     * A carrier holding all of it leaves nothing for a market, which is the one case where the commander is
     * told about the carrier at all - there is nothing to buy and nowhere to send them.
     */
    @Test
    void aCarrierCoveringTheBuildLeavesNothingToSearchFor() {
        assertTrue(ConstructionShopping.stillToAcquire(wittHub(Map.of()),
                        new Stash("GHY-L8X", Map.of("aluminium", 1858, "steel", 600, "titanium", 240)))
                .isEmpty());
    }

    /**
     * Spansh matches on the commodities table's spelling, so a good it cannot be given is a good no market
     * can be searched for. The rest of the list still goes out.
     */
    @Test
    void aGoodWithNoNameInOurTablesIsDroppedRatherThanSearchedFor() {
        List<Outstanding> manifest = ConstructionCargo.outstanding(List.of(
                line("notacommodity", 9000, 0),
                line("steel", 600, 0)), Map.of());

        List<WantedCommodity> wanted = shoppingList(manifest, null);

        assertEquals(List.of("steel"), wanted.stream().map(WantedCommodity::symbol).toList());
    }
}
