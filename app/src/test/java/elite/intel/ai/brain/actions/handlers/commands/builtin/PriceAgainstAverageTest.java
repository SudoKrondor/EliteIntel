package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.managers.CommodityMeanPriceManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The sentence that turns a number into a judgement. "57,844 credits per unit" tells the commander nothing
 * they can act on; "13 percent above the galactic average of 51,294" tells them whether to sell or fly on.
 *
 * <p>Stated as a plain percentage rather than as praise, because above average is a good sale and a dear
 * purchase - and because whether the trade shows a PROFIT needs the cost basis, which the journal never
 * gives: {@code CargoTransfer}, how a carrier owner loads most of what they sell, carries no price at all.
 */
class PriceAgainstAverageTest {

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        SystemSession.getInstance().setLanguage(Language.EN);
        CommodityMeanPriceManager.getInstance().harvest(GsonFactory.getGson().fromJson("""
                {"timestamp":"2026-08-28T21:07:10Z","event":"Market","MarketID":4335542787,
                 "StationName":"Love Hub","Items":[
                   {"Name":"$tritium_name;","MeanPrice":51294}]}""", GameEvents.MarketEvent.class));
    }

    @Test
    void aGoodSaleIsQuotedAgainstTheAverage() {
        // The real figures: Love Hub pays 57,844 against a galactic average of 51,294.
        String verdict = CommodityTradeSearch.againstGalacticAverage("Tritium", 57844);

        assertTrue(verdict.contains("13 percent above"), verdict);
        assertTrue(verdict.contains("51,294") || verdict.contains("51294"), verdict);
    }

    @Test
    void aPoorPriceIsQuotedTheSameWay() {
        String verdict = CommodityTradeSearch.againstGalacticAverage("Tritium", 40000);

        assertTrue(verdict.contains("22 percent below"), verdict);
    }

    @Test
    void anOrdinaryPriceIsNotDressedUpInPercentages() {
        // Two percent off the mean is an ordinary market, and a percentage there reads as precision the
        // number does not carry.
        String verdict = CommodityTradeSearch.againstGalacticAverage("Tritium", 52000);

        assertTrue(verdict.contains("about the galactic average"), verdict);
        assertFalse(verdict.contains("percent"), verdict);
    }

    @Test
    void aGoodWithNoKnownAverageSaysNothingRatherThanGuessing() {
        assertEquals("", CommodityTradeSearch.againstGalacticAverage("Nonexistent Widget", 57844));
    }

    @Test
    void aMissingPriceSaysNothing() {
        assertEquals("", CommodityTradeSearch.againstGalacticAverage("Tritium", 0));
    }

    @Test
    void theVerdictIsAppendedNotSubstituted() {
        assertTrue(CommodityTradeSearch.againstGalacticAverage("Tritium", 57844).startsWith(" "),
                "it joins the end of an answer that already says where to go");
    }
}
