package elite.intel.junit.db;

import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.MaterialsDao;
import elite.intel.db.managers.MaterialManager;
import elite.intel.db.util.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FuzzySearchTest {

    // materials is game-state — empty after migration; seed what we need.
    @BeforeEach
    void seedMaterials() {
        MaterialManager.getInstance().clear();
        Database.withDao(MaterialsDao.class, dao -> {
            dao.upsert("Carbon", "Raw", 10, 300);
            dao.upsert("Iron", "Raw", 5, 300);
            dao.upsert("Nickel", "Raw", 3, 300);
            return null;
        });
    }

    @AfterEach
    void clearMaterials() {
        MaterialManager.getInstance().clear();
    }

    // ── levenshteinDistance ────────────────────────────────────────────────

    @Test
    void levenshteinExactMatchIsZero() {
        assertEquals(0, FuzzySearch.levenshteinDistance("carbon", "carbon"));
    }

    @Test
    void levenshteinOneSubstitution() {
        // "cardon" differs from "carbon" by one char swap
        assertEquals(1, FuzzySearch.levenshteinDistance("carbon", "cardon"));
    }

    @Test
    void levenshteinOneDeletion() {
        assertEquals(1, FuzzySearch.levenshteinDistance("carbon", "carbn"));
    }

    @Test
    void levenshteinOneInsertion() {
        assertEquals(1, FuzzySearch.levenshteinDistance("iron", "irons"));
    }

    @Test
    void levenshteinEmptyVsWord() {
        assertEquals(4, FuzzySearch.levenshteinDistance("", "iron"));
    }

    // ── fuzzyMaterialNameSearch (material_names is migration-seeded ref data) ──

    @Test
    void materialNameSearchExactInputReturnsCanonicalCase() {
        assertEquals("Carbon", FuzzySearch.fuzzyMaterialNameSearch("carbon", 8));
    }

    @Test
    void materialNameSearchPrefixReturnsShortestMatch() {
        // "iro" is an unambiguous prefix of "Iron" only
        assertEquals("Iron", FuzzySearch.fuzzyMaterialNameSearch("iro", 8));
    }

    @Test
    void materialNameSearchOneTypoStillMatches() {
        // "carbn" — one deletion from "carbon"; Levenshtein distance = 1
        assertEquals("Carbon", FuzzySearch.fuzzyMaterialNameSearch("carbn", 8));
    }

    @Test
    void materialNameSearchTotallyUnknownReturnsNull() {
        assertNull(FuzzySearch.fuzzyMaterialNameSearch("xxxxxxxxxx", 2));
    }

    // ── fuzzyCommodityMatch (commodities is migration-seeded ref data) ─────

    @Test
    void commodityMatchExactInputReturnsCanonicalCase() {
        assertEquals("Gold", FuzzySearch.fuzzyCommodityMatch("gold", 3));
    }

    @Test
    void commodityMatchPrefixReturnsShortest() {
        // "trit" is an unambiguous prefix of "Tritium"
        assertEquals("Tritium", FuzzySearch.fuzzyCommodityMatch("trit", 3));
    }

    @Test
    void commodityMatchOneTypoStillMatches() {
        // "platnum" → "Platinum": insert 'i' → distance 1
        assertEquals("Platinum", FuzzySearch.fuzzyCommodityMatch("platnum", 3));
    }

    @Test
    void commodityMatchUnknownReturnsNull() {
        assertNull(FuzzySearch.fuzzyCommodityMatch("xxxxxxxxxx", 3));
    }

    // ── fuzzyInventorySearch (materials table, game-state, seeded above) ───

    @Test
    void inventorySearchExactInputReturnsCanonicalCase() {
        assertEquals("Carbon", FuzzySearch.fuzzyInventorySearch("carbon", 8));
    }

    @Test
    void inventorySearchPrefixMatchesShortestCandidate() {
        // "iro" prefixes "Iron" but not "Carbon" or "Nickel"
        assertEquals("Iron", FuzzySearch.fuzzyInventorySearch("iro", 8));
    }

    @Test
    void inventorySearchOneTypoStillMatches() {
        // "nickl" → "Nickel": distance 1
        assertEquals("Nickel", FuzzySearch.fuzzyInventorySearch("nickl", 8));
    }

    @Test
    void inventorySearchCommodityNotInMaterialsReturnsNull() {
        // "Gold" is a commodity, not in the materials inventory
        assertNull(FuzzySearch.fuzzyInventorySearch("gold", 8));
    }
}
