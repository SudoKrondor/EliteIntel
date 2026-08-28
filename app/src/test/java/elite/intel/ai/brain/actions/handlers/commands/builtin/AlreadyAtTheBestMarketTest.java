package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.util.Database;
import elite.intel.gameapi.search.spansh.commodity.CommoditySearchResult;
import elite.intel.i18n.Language;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The best market in range is very often the pad the ship is already parked on - most of all on a SECOND
 * search, made after flying to the first answer and not liking the price.
 *
 * <p>That port is not an answer. The command's promise is a plotted course, and the plotter cannot plot into
 * the system it is already in, so naming it produces a destination with no galaxy map behind it - and the
 * commander cannot type "Col 285 Sector IB-X d1-60" by hand, least of all in VR. A commander who asks where
 * to sell while docked is asking where ELSE, so the pad under the ship is dropped from the candidates and
 * the course goes to the best market that can actually be flown to.
 */
class AlreadyAtTheBestMarketTest {

    private static final String HERE = "Col 285 Sector IB-X d1-32";

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @BeforeEach
    void dockAtLoveHub() {
        PlayerSession.getInstance().setCurrentPrimaryStarName(HERE);
        DockedMarket.getInstance().arrived(4335542787L, "Love Hub");
    }

    @AfterEach
    void undock() {
        DockedMarket.getInstance().departed();
    }

    @Test
    void thePadUnderTheShipIsRecognised() {
        assertTrue(CommodityTradeSearch.standingOn(market("Love Hub", HERE, 57844, 0)));
    }

    @Test
    void aStationOfTheSameNameInAnotherSystemIsNotThisOne() {
        // Station names repeat across the galaxy, and DockedMarket only knows the name - so the system has
        // to agree before a market 80 ly away is mistaken for the one under the ship.
        assertFalse(CommodityTradeSearch.standingOn(market("Love Hub", "Hyades Sector MH-V c2-8", 57844, 41)));
    }

    @Test
    void anotherStationInThisSystemIsNotThisOne() {
        assertFalse(CommodityTradeSearch.standingOn(market("Charybdis Dio", HERE, 57823, 0)));
    }

    @Test
    void nothingIsMistakenForAPadWhileFlying() {
        DockedMarket.getInstance().departed();
        assertFalse(CommodityTradeSearch.standingOn(market("Love Hub", HERE, 57844, 0)));
    }

    @Test
    void theTopMarketIsSkippedWhenItIsThePadWeAreOn() {
        List<CommoditySearchResult> flyable = CommodityTradeSearch.awayFromThisPad(List.of(
                market("Love Hub", HERE, 57844, 0),
                market("Henslow Horizons", "HR 1975", 57833, 40)));

        assertEquals(1, flyable.size());
        assertEquals("Henslow Horizons", flyable.getFirst().getStationName(),
                "the course has to go somewhere the ship can actually fly");
    }

    @Test
    void everyRowForThisPadIsDroppedNotJustTheFirst() {
        List<CommoditySearchResult> flyable = CommodityTradeSearch.awayFromThisPad(List.of(
                market("Love Hub", HERE, 57844, 0),
                market("Love Hub", HERE, 57844, 0),
                market("Charybdis Dio", "Hyades Sector IB-X c1-15", 57823, 20)));

        assertEquals(1, flyable.size());
        assertEquals("Charybdis Dio", flyable.getFirst().getStationName());
    }

    @Test
    void aPageWithNowhereElseToGoComesBackEmpty() {
        assertTrue(CommodityTradeSearch.awayFromThisPad(
                        List.of(market("Love Hub", HERE, 57844, 0))).isEmpty(),
                "the caller says so plainly rather than plotting a course to the pad under the ship");
    }

    @Test
    void everyCandidateSurvivesWhileTheShipIsFlying() {
        DockedMarket.getInstance().departed();
        List<CommoditySearchResult> markets = List.of(
                market("Love Hub", HERE, 57844, 0),
                market("Henslow Horizons", "HR 1975", 57833, 40));

        assertEquals(markets, CommodityTradeSearch.awayFromThisPad(markets));
    }

    private static CommoditySearchResult market(String station, String system, double price, double ly) {
        CommoditySearchResult result = new CommoditySearchResult();
        result.setStationName(station);
        result.setStarSystem(system);
        result.setPrice(price);
        result.setDistanceFromPlayer(ly);
        return result;
    }
}
