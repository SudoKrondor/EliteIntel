package elite.intel.gameapi.colonisation;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shopping list a construction site's manifest turns into.
 * <p>
 * The manifest in these fixtures is the real one from Orbital Construction Site: Divis Gateway, which is
 * what the feature was built against.
 */
class ConstructionCargoTest {

    private static Requirement line(String symbol, int required, int provided, long payment) {
        Requirement requirement = new Requirement();
        requirement.setMarketId(3967232514L);
        requirement.setSymbol(symbol);
        requirement.setGameName(symbol);
        requirement.setRequiredAmount(required);
        requirement.setProvidedAmount(provided);
        requirement.setPayment(payment);
        return requirement;
    }

    /**
     * The manifest as read off the depot panel, before anything was hauled to it.
     */
    private static List<Requirement> divisGateway() {
        return List.of(
                line("steel", 2542, 0, 5057),
                line("titanium", 1525, 0, 5360),
                line("aluminium", 1322, 0, 3239),
                line("liquidoxygen", 678, 0, 2260),
                line("polymers", 170, 0, 682),
                line("insulatingmembrane", 106, 0, 11788),
                line("computercomponents", 22, 22, 1112),
                line("fruitandvegetables", 9, 0, 865));
    }

    @Test
    void theLongestPoleIsWhatTheCommanderIsSentShoppingFor() {
        Optional<ConstructionCargo.Outstanding> next =
                ConstructionCargo.nextToSource(divisGateway(), Map.of());

        assertTrue(next.isPresent());
        assertEquals("steel", next.get().symbol());
        assertEquals(2542, next.get().shortfall());
    }

    @Test
    void aFullyDeliveredLineIsNotShopping() {
        List<ConstructionCargo.Outstanding> outstanding =
                ConstructionCargo.outstanding(divisGateway(), Map.of());

        assertTrue(outstanding.stream().noneMatch(line -> line.symbol().equals("computercomponents")),
                "a line the site already has in full is finished, not outstanding");
        assertEquals(7, outstanding.size());
    }

    @Test
    void cargoAlreadyInTheHoldIsDeductedFromTheShortfallButNotFromWhatTheSiteWants() {
        List<ConstructionCargo.Outstanding> outstanding =
                ConstructionCargo.outstanding(divisGateway(), Map.of("steel", 400));

        ConstructionCargo.Outstanding steel = outstanding.getFirst();
        assertEquals("steel", steel.symbol());
        assertEquals(400, steel.held());
        assertEquals(2142, steel.shortfall(), "what is still to buy");
        assertEquals(2542, steel.outstanding(), "what the site still wants, hold or no hold");
    }

    @Test
    void aHoldThatCoversTheBiggestLineMovesTheShoppingOnToTheNextOne() {
        Optional<ConstructionCargo.Outstanding> next =
                ConstructionCargo.nextToSource(divisGateway(), Map.of("steel", 3000));

        assertTrue(next.isPresent());
        assertEquals("titanium", next.get().symbol(),
                "steel is covered by the hold, so the next trip is for the next largest");
    }

    @Test
    void aHoldCarryingMoreThanTheLineWantsDoesNotCreditTheSurplusAnywhereElse() {
        List<ConstructionCargo.Outstanding> outstanding =
                ConstructionCargo.outstanding(divisGateway(), Map.of("fruitandvegetables", 500));

        ConstructionCargo.Outstanding fruit = outstanding.stream()
                .filter(line -> line.symbol().equals("fruitandvegetables"))
                .findFirst()
                .orElseThrow();
        assertEquals(9, fruit.held(), "the hold can only claim what the site actually wants");
        assertTrue(fruit.isSatisfied());
    }

    @Test
    void anOverDeliveredLineIsFinishedRatherThanOwedBackwards() {
        List<ConstructionCargo.Outstanding> outstanding =
                ConstructionCargo.outstanding(List.of(line("water", 22, 30, 662)), Map.of());

        assertTrue(outstanding.isEmpty(), "a site given more than it asked for wants nothing");
    }

    @Test
    void theOrderIsStableWhenTwoLinesOweTheSameTonnage() {
        List<Requirement> tie = List.of(
                line("titanium", 100, 0, 5360),
                line("aluminium", 100, 0, 3239));

        assertEquals(List.of("aluminium", "titanium"),
                ConstructionCargo.outstanding(tie, Map.of()).stream()
                        .map(ConstructionCargo.Outstanding::symbol)
                        .toList(),
                "a tie has to break the same way every poll, or the card and the voice disagree");
    }

    @Test
    void aFinishedManifestSendsNobodyShopping() {
        assertTrue(ConstructionCargo.nextToSource(
                List.of(line("steel", 2542, 2542, 5057)), Map.of()).isEmpty());
    }

    @Test
    void anEmptyOrNullManifestIsQuietRatherThanFatal() {
        assertTrue(ConstructionCargo.outstanding(null, null).isEmpty());
        assertTrue(ConstructionCargo.outstanding(List.of(), Map.of()).isEmpty());
    }
}
