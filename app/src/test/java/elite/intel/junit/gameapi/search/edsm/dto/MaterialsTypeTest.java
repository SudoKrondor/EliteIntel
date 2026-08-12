package elite.intel.junit.gameapi.search.edsm.dto;

import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import org.junit.jupiter.api.Test;

import static elite.intel.gameapi.search.edsm.dto.MaterialsType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The journal spells the same three categories three ways; all of them must land on the same type.
 */
class MaterialsTypeTest {

    @Test
    void titleCaseCategoryField() {
        assertEquals(GAME_RAW, MaterialsType.fromJournalCategory("Raw"));
        assertEquals(GAME_MANUFACTURED, MaterialsType.fromJournalCategory("Manufactured"));
        assertEquals(GAME_ENCODED, MaterialsType.fromJournalCategory("Encoded"));
    }

    @Test
    void lowerCaseTraderType() {
        // MaterialTrade's TraderType.
        assertEquals(GAME_RAW, MaterialsType.fromJournalCategory("raw"));
        assertEquals(GAME_MANUFACTURED, MaterialsType.fromJournalCategory("manufactured"));
        assertEquals(GAME_ENCODED, MaterialsType.fromJournalCategory("encoded"));
    }

    @Test
    void gameToken() {
        // MissionCompleted's MaterialsReward.
        assertEquals(GAME_MANUFACTURED, MaterialsType.fromJournalCategory("$MICRORESOURCE_CATEGORY_Manufactured;"));
        assertEquals(GAME_RAW, MaterialsType.fromJournalCategory("$MICRORESOURCE_CATEGORY_Raw;"));
        assertEquals(GAME_ENCODED, MaterialsType.fromJournalCategory("$MICRORESOURCE_CATEGORY_Encoded;"));
    }

    @Test
    void unknownOrAbsentCategoryDoesNotThrow() {
        // The category only labels a material; a strange one must not cost us the count.
        assertEquals(GAME_UNKNOWN, MaterialsType.fromJournalCategory(null));
        assertEquals(GAME_UNKNOWN, MaterialsType.fromJournalCategory(""));
        assertEquals(GAME_UNKNOWN, MaterialsType.fromJournalCategory("Thargoid"));
        assertEquals(GAME_UNKNOWN, MaterialsType.fromJournalCategory("$MICRORESOURCE_CATEGORY_Item;"));
    }
}
