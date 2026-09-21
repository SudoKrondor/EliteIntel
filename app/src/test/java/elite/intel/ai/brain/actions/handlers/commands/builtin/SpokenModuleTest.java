package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.dao.CommodityDao;
import elite.intel.db.util.Database;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The designation is taken off the spoken words and the module's name is what is left - every way a pilot
 * says it, because the reflex and the LLM both hand the command the sentence as spoken.
 */
class SpokenModuleTest {

    @Test
    void sizeAndClassAsThePilotSaysThem() {
        SpokenModule module = SpokenModule.parse("fuel scoop size 6 class B");

        assertEquals("fuel scoop", module.name());
        assertEquals(6, module.size());
        assertEquals("B", module.rating());
        assertNull(module.mount());
        assertTrue(module.isSpecific());
    }

    @Test
    void sizeAloneLeavesTheClassOpen() {
        SpokenModule module = SpokenModule.parse("fuel scoop size 6");

        assertEquals("fuel scoop", module.name());
        assertEquals(6, module.size());
        assertNull(module.rating());
    }

    /**
     * "class 6" is how many pilots say the SIZE - the digit decides which half of the designation it is.
     */
    @Test
    void classFollowedByADigitIsTheSize() {
        SpokenModule module = SpokenModule.parse("class 6 fuel scoop");

        assertEquals("fuel scoop", module.name());
        assertEquals(6, module.size());
        assertNull(module.rating());
    }

    @Test
    void theScreenDesignationIsUnderstood() {
        assertEquals(new SpokenModule("fuel scoop", 6, "B", null), SpokenModule.parse("6b fuel scoop"));
        assertEquals(new SpokenModule("fuel scoop", 6, "B", null), SpokenModule.parse("fuel scoop 6 b"));
    }

    @Test
    void aRatedClassAndAGradeAreTheClass() {
        assertEquals("A", SpokenModule.parse("a rated power plant").rating());
        assertEquals("A", SpokenModule.parse("a-rated power plant").rating());
        assertEquals("C", SpokenModule.parse("grade c sensors").rating());
        assertEquals("power plant", SpokenModule.parse("a rated power plant").name());
    }

    /**
     * The article in "a fuel scoop" is not a class A: only a letter carrying a class word, or standing in a
     * designation with a digit, is a class.
     */
    @Test
    void theArticleIsNotAClass() {
        SpokenModule module = SpokenModule.parse("a fuel scoop");

        assertNull(module.rating());
        assertFalse(module.isSpecific());
        assertEquals("a fuel scoop", module.name());
    }

    @Test
    void theMountIsTakenInSpanshsWord() {
        assertEquals(new SpokenModule("beam laser", null, null, "Gimbal"), SpokenModule.parse("gimballed beam laser"));
        assertEquals("Turret", SpokenModule.parse("turreted multi-cannon").mount());
        assertEquals("Fixed", SpokenModule.parse("fixed pulse laser").mount());
    }

    @Test
    void theWordModuleNamesNothing() {
        assertEquals("fuel scoop", SpokenModule.parse("fuel scoop module").name());
    }

    @Test
    void aCommodityPassesThroughUntouched() {
        SpokenModule module = SpokenModule.parse("gold");

        assertEquals("gold", module.name());
        assertFalse(module.isSpecific());
    }

    /**
     * A designation found in a commodity's name would send the good down the module branch, where it is
     * unbuyable - so no commodity the catalogue seeds may read as one.
     */
    @Test
    void noCommodityReadsAsAModuleDesignation() {
        List<String> commodities = Database.withDao(CommodityDao.class, CommodityDao::getAllNamesLowerCase);
        assertFalse(commodities.isEmpty());
        for (String commodity : commodities) {
            SpokenModule module = SpokenModule.parse(commodity);
            assertFalse(module.isSpecific(), () -> commodity + " parsed as " + module);
            assertEquals(commodity, module.name());
        }
    }

    @Test
    void nothingSaidIsNothingParsed() {
        SpokenModule module = SpokenModule.parse(null);

        assertEquals("", module.name());
        assertFalse(module.isSpecific());
    }
}
