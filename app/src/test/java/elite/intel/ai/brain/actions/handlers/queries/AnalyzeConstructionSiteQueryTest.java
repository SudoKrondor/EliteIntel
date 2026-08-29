package elite.intel.ai.brain.actions.handlers.queries;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ConstructionCargo;
import elite.intel.gameapi.colonisation.ConstructionCargo.Outstanding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tonnages this query hands the model. Everything asserted here is a figure the model is told to read
 * rather than to work out, so what this pins is precisely what a small model cannot be trusted to do itself.
 */
class AnalyzeConstructionSiteQueryTest {

    private static final long MARKET_ID = 3964071938L;

    private static Requirement line(String symbol, int required, int provided) {
        Requirement requirement = new Requirement();
        requirement.setMarketId(MARKET_ID);
        requirement.setSymbol(symbol);
        requirement.setGameName(symbol);
        requirement.setRequiredAmount(required);
        requirement.setProvidedAmount(provided);
        requirement.setPayment(5360);
        return requirement;
    }

    /**
     * Witt Hub as it stood on the live run: titanium 6,161 still wanted, 5,281 of it already on the carrier.
     */
    private static List<Outstanding> wittHub(Map<String, Integer> hold) {
        return ConstructionCargo.outstanding(List.of(
                line("titanium", 7921, 1760), line("aluminium", 4177, 0), line("water", 2343, 734)), hold);
    }

    private static AnalyzeConstructionSiteQuery.LineDto lineFor(
            List<AnalyzeConstructionSiteQuery.LineDto> lines, String commodity) {
        return lines.stream()
                .filter(line -> line.commodity().equalsIgnoreCase(commodity))
                .findFirst()
                .orElseThrow(() -> new AssertionError(commodity + " missing from " + lines));
    }

    @Test
    @DisplayName("what is left to buy is subtracted here, not left to the model")
    void subtractsWhatIsAlreadyBought() {
        List<AnalyzeConstructionSiteQuery.LineDto> lines = AnalyzeConstructionSiteQuery.report(
                wittHub(Map.of()), new Stash("GHY-L8X", Map.of("titanium", 5281)), Set.of("titanium"));

        AnalyzeConstructionSiteQuery.LineDto titanium = lineFor(lines, "titanium");
        assertEquals(6161, titanium.outstandingTonnes(), "what the site still wants in total");
        assertEquals(5281, titanium.ownedTonnes(), "already bought, and sitting on the carrier");
        assertEquals(880, titanium.stillToBuyTonnes(), "the answer to \"how much do we still need to buy\"");
    }

    /**
     * Cargo on the carrier is bought cargo. Counting only the ship's hold is what had the voice saying
     * "missing 6,161 tonnes of titanium" over a card reading 5,281/6,161.
     */
    @Test
    @DisplayName("the carrier counts, and so does the ship's hold")
    void countsTheCarrierAndTheHoldTogether() {
        List<AnalyzeConstructionSiteQuery.LineDto> lines = AnalyzeConstructionSiteQuery.report(
                wittHub(Map.of("titanium", 400)), new Stash("GHY-L8X", Map.of("titanium", 5281)),
                Set.of("titanium"));

        assertEquals(5681, lineFor(lines, "titanium").ownedTonnes());
        assertEquals(480, lineFor(lines, "titanium").stillToBuyTonnes());
    }

    @Test
    @DisplayName("with no carrier the hold is all there is")
    void toleratesNoCarrier() {
        List<AnalyzeConstructionSiteQuery.LineDto> lines = AnalyzeConstructionSiteQuery.report(
                wittHub(Map.of("titanium", 400)), null, Set.of());

        assertEquals(400, lineFor(lines, "titanium").ownedTonnes());
        assertEquals(5761, lineFor(lines, "titanium").stillToBuyTonnes());
    }

    /**
     * "How much do we need to buy HERE" is only answerable about goods this market sells, and whether it
     * sells one is a fact off the shelves rather than something to infer from a name.
     */
    @Test
    @DisplayName("each line says whether this market stocks it")
    void marksWhatThisMarketSells() {
        List<AnalyzeConstructionSiteQuery.LineDto> lines = AnalyzeConstructionSiteQuery.report(
                wittHub(Map.of()), null, Set.of("titanium", "water"));

        assertTrue(lineFor(lines, "titanium").soldHere());
        assertTrue(lineFor(lines, "water").soldHere());
        assertFalse(lineFor(lines, "aluminium").soldHere(), "not on these shelves");
    }

    /**
     * A good bought in full is not a shopping answer of "buy minus six hundred": it is zero, and the
     * instructions turn that into "already bought, go and deliver it".
     */
    @Test
    @DisplayName("a good bought in full asks for nothing, never a negative")
    void flooringAtZero() {
        List<AnalyzeConstructionSiteQuery.LineDto> lines = AnalyzeConstructionSiteQuery.report(
                wittHub(Map.of()), new Stash("GHY-L8X", Map.of("titanium", 7000)), Set.of("titanium"));

        assertEquals(0, lineFor(lines, "titanium").stillToBuyTonnes());
    }

    /**
     * A question about the tail of the manifest cannot be answered from a payload the tail was trimmed out
     * of, so an ordinary build reaches the model whole.
     */
    @Test
    @DisplayName("a whole ordinary manifest reaches the model")
    void keepsTheSmallLinesTheQuestionMayBeAbout() {
        List<Outstanding> wholeBuild = ConstructionCargo.outstanding(List.of(
                line("steel", 8434, 6160), line("titanium", 7921, 880), line("aluminium", 4177, 0),
                line("water", 1609, 0), line("ceramiccomposites", 1207, 0), line("computercomponents", 145, 0),
                line("fruitandvegetables", 145, 0), line("waterpurifiers", 105, 0)), Map.of());

        List<AnalyzeConstructionSiteQuery.LineDto> lines =
                AnalyzeConstructionSiteQuery.report(wholeBuild, null, Set.of());

        assertEquals(8, lines.size());
        assertTrue(lines.stream().noneMatch(line -> line.commodity().equals("fruitandvegetables")),
                "a journal symbol in the payload is a journal symbol read out loud");
        assertEquals(105, lineFor(lines, "Water Purifiers").stillToBuyTonnes(),
                "the smallest line on the build, and still answerable");
    }

    /**
     * The order a haul is worked: what is under way first, then the largest requirement.
     */
    @Test
    @DisplayName("a good already part bought leads the list")
    void ordersStartedGoodsFirst() {
        List<AnalyzeConstructionSiteQuery.LineDto> lines = AnalyzeConstructionSiteQuery.report(
                wittHub(Map.of()), new Stash("GHY-L8X", Map.of("water", 500)), Set.of());

        assertEquals("Water", lines.getFirst().commodity(),
                "1,609 short against titanium's 6,161, but it is the one under way");
    }
}
