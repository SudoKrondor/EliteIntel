package elite.intel.gameapi.journal.events.dto;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.RankEvent;
import elite.intel.util.Ranks;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Elite in at least one career" is what opens Shinrarta Dezhra, and the career ranks are otherwise stored
 * as localized names, so the number that answers it is pinned here: taken from the Rank snapshot across
 * every career, raised by a promotion in any of them, and unknown until the game has spoken.
 */
class RankAndProgressDtoCareerRankTest {

    private static RankEvent rank(int combat, int trade, int explore, int soldier, int exobiologist, int cqc) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2026-09-20T12:00:00Z");
        json.addProperty("event", "Rank");
        json.addProperty("Combat", combat);
        json.addProperty("Trade", trade);
        json.addProperty("Explore", explore);
        json.addProperty("Soldier", soldier);
        json.addProperty("Exobiologist", exobiologist);
        json.addProperty("Empire", 0);
        json.addProperty("Federation", 0);
        json.addProperty("CQC", cqc);
        return new RankEvent(json);
    }

    @Test
    @DisplayName("unknown until the game reports the ranks, and unknown is not Elite")
    void unknownIsNotElite() {
        RankAndProgressDto dto = new RankAndProgressDto();
        assertEquals(-1, dto.getHighestCareerRank());
        assertFalse(dto.hasEliteRank());
    }

    @Test
    @DisplayName("the snapshot takes the highest across every career, CQC included")
    void snapshotTakesHighest() {
        RankAndProgressDto dto = new RankAndProgressDto();
        dto.setRanksData(rank(3, 2, 7, 1, 0, 0));
        assertEquals(7, dto.getHighestCareerRank());
        assertFalse(dto.hasEliteRank());

        dto.setRanksData(rank(3, 2, 7, 1, 0, Ranks.ELITE));
        assertTrue(dto.hasEliteRank());
    }

    @Test
    @DisplayName("a promotion raises the highest rank and a promotion elsewhere never lowers it")
    void promotionRaisesOnly() {
        RankAndProgressDto dto = new RankAndProgressDto();
        dto.setRanksData(rank(7, 0, 0, 0, 0, 0));
        dto.raiseCareerRank(Ranks.ELITE);
        assertTrue(dto.hasEliteRank());
        dto.raiseCareerRank(2);
        dto.raiseCareerRank(null);
        assertEquals(Ranks.ELITE, dto.getHighestCareerRank());
    }

    @Test
    @DisplayName("a stored row from before the field existed reads as unknown, not as unranked")
    void legacyRowReadsUnknown() {
        RankAndProgressDto dto = GsonFactory.getGson().fromJson("{\"combatRank\":\"Elite\"}", RankAndProgressDto.class);
        assertEquals(-1, dto.getHighestCareerRank());
        assertFalse(dto.hasEliteRank());
    }
}
