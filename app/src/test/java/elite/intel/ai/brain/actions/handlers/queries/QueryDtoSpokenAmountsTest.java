package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.gameapi.search.edsm.dto.data.Commodity;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.yaml.YamlFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The query DTOs speak their credit amounts the same way the finance announcements do: each numeric field
 * gets a rounded, spelled-out {@code ...Spoken} sibling appended to the serialized YAML (see
 * {@code SpokenAmounts.RULE}). These tests pin that the sibling lands as a top-level key holding the value
 * matching its numeric field, including for {@code AnalyzePlayerProfileQuery} whose DTO also carries a nested
 * object, where a mis-indented append would fold the sibling into the wrong block.
 */
class QueryDtoSpokenAmountsTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String yaml) throws Exception {
        return YamlFactory.getMapper().readValue(yaml, Map.class);
    }

    @Test
    void playerProfileAmountsGetTopLevelSpokenSiblingsAlongsideTheNestedRankBlock() throws Exception {
        var dto = new AnalyzePlayerProfileQuery.DataDto(
                new RankAndProgressDto(), "Admiral",
                1_023_309_245L, 5_000_000L, 12_449_000L, 342_600L,
                1234.5, 999L, 42L, 8_450L, 200_000L);

        Map<String, Object> parsed = parse(dto.toYaml());

        // The nested record still serializes as a nested block, and the flat siblings sit beside it.
        assertTrue(parsed.get("data") instanceof Map, "nested rank block must survive the append");
        assertEquals("about one point zero two billion credits", parsed.get("totalBountiesCollectedSpoken"));
        // 8,450 is under the exact threshold, so it is spoken precisely with no "about" hedge.
        assertEquals("eight thousand four hundred fifty credits", parsed.get("totalExobiologyProfitsSpoken"));
        // The raw numeric field is retained so the exact figure stays answerable on request.
        assertEquals(1_023_309_245L, ((Number) parsed.get("totalBountiesCollected")).longValue());
    }

    @Test
    void carrierStatusSpeaksEveryBankBalance() throws Exception {
        var dto = new AnalyzeCarrierStatusQuery.DataDto(
                "your fleet carrier", 1_000_000L, 2_000_000_000L, -500_000L,
                100, true, 50, 150, 400, 500, 3);

        Map<String, Object> parsed = parse(dto.toYaml());

        assertEquals("about one million credits", parsed.get("reserveBalanceSpoken"));
        assertEquals("about two billion credits", parsed.get("totalBalanceSpoken"));
        assertEquals("minus about five hundred thousand credits", parsed.get("marketBalanceSpoken"));
    }

    @Test
    void bountiesAndExplorationProfitsSpeakTheirTotals() throws Exception {
        Map<String, Object> bounties = parse(new AnalyzeBountiesCollectedQuery.DataDto(4_980_000L).toYaml());
        assertEquals("about five million credits", bounties.get("totalBountiesSpoken"));

        Map<String, Object> profits = parse(new AnalyzeExplorationProfitsQuery.DataDto(342_600L, 0L, 1_500_000L).toYaml());
        assertEquals("about three hundred forty-three thousand credits", profits.get("potentialProfitSpoken"));
        assertEquals("about one point five million credits", profits.get("unconfirmedFirstDiscoveryBonusSpoken"));
    }

    /**
     * A zero figure is not in the payload at all, so the model cannot read out "potential profit is zero".
     */
    @Test
    void explorationProfitsLeaveZeroFiguresOut() throws Exception {
        Map<String, Object> profits = parse(new AnalyzeExplorationProfitsQuery.DataDto(0L, 11_985_700L, 0L).toYaml());
        assertEquals("about twelve million credits", profits.get("acquiredProfitSpoken"));
        assertFalse(profits.containsKey("potentialProfit"));
        assertFalse(profits.containsKey("potentialProfitSpoken"));
        assertFalse(profits.containsKey("unconfirmedFirstDiscoveryBonus"));
        assertFalse(profits.containsKey("unconfirmedFirstDiscoveryBonusSpoken"));
    }

    @Test
    void pirateMissionProfitIsANumberWithASpokenSibling() throws Exception {
        Map<String, Object> pirate = parse(new AnalyzePirateMissionQuery.DataDto("12 kills left", 11_000_700L).toYaml());
        assertEquals(11_000_700, ((Number) pirate.get("totalMissionProfit")).longValue());
        assertEquals("about eleven million credits", pirate.get("totalMissionProfitSpoken"));
    }

    /**
     * Nested rows (market lines, missions, depot lines) carry their sibling beside the numeric field, and only
     * when rounding changes it: a price said exactly anyway would just double a hundred-line market.
     */
    @Test
    @SuppressWarnings("unchecked")
    void nestedRowsGetASpokenSiblingOnlyWhenTheAmountIsRounded() throws Exception {
        Commodity tritium = new Commodity();
        tritium.buyPrice = 57_844;
        tritium.sellPrice = 4_120;
        Map<String, Object> row = parse(YamlFactory.toYaml(tritium));
        assertEquals("about fifty-eight thousand credits", row.get("buyPriceSpoken"));
        assertFalse(row.containsKey("sellPriceSpoken"));

        MissionDto mission = new MissionDto(new MissionAcceptedEvent(GsonFactory.getGson().fromJson("""
                {"timestamp":"2026-08-08T03:04:09Z","event":"MissionAccepted","Name":"Mission_Courier",
                 "MissionID":1,"Expiry":"2026-08-09T03:03:10Z","Reward":11985700}
                """, JsonObject.class)));
        assertEquals("about twelve million credits", parse(YamlFactory.toYaml(mission)).get("rewardSpoken"));

        var line = new AnalyzeConstructionSiteQuery.LineDto("Steel", 100, 0, 100, true, 15_400L);
        var depot = parse(YamlFactory.toYaml(line));
        assertEquals("about fifteen thousand credits", depot.get("paymentPerTonneSpoken"));
    }
}
