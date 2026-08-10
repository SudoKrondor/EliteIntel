package elite.intel.gameapi.hge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The High Grade Emissions drop table, as the community wiki documents it. These are Frontier's
 * rules, so the cases here are worth stating explicitly: a wrong entry sends the commander hunting
 * for a material the system cannot produce.
 */
class HighGradeEmissionsTest {

    private static final long POPULATED = 5_000_000L;

    @Test
    @DisplayName("federal systems offer the two Federal composites")
    void federalSystems() {
        assertEquals(List.of("fedcorecomposites", "fedproprietarycomposites"),
                HighGradeEmissions.materialSymbols("Federation", List.of(), POPULATED));
    }

    @Test
    @DisplayName("imperial systems offer Imperial Shielding")
    void imperialSystems() {
        assertEquals(List.of("imperialshielding"),
                HighGradeEmissions.materialSymbols("Empire", List.of(), POPULATED));
    }

    @Test
    @DisplayName("an independent or alliance system offers nothing on allegiance alone")
    void neutralAllegianceOffersNothing() {
        assertTrue(HighGradeEmissions.materialSymbols("Independent", List.of(), POPULATED).isEmpty());
        assertTrue(HighGradeEmissions.materialSymbols("Alliance", List.of(), POPULATED).isEmpty());
        assertTrue(HighGradeEmissions.materialSymbols(null, null, POPULATED).isEmpty());
    }

    @Test
    @DisplayName("civil unrest offers Improvised Components")
    void civilUnrest() {
        assertEquals(List.of("improvisedcomponents"),
                HighGradeEmissions.materialSymbols("Independent", List.of("CivilUnrest"), POPULATED));
    }

    @Test
    @DisplayName("war and civil war both offer the military pair")
    void warStates() {
        List<String> military = List.of("militarygradealloys", "militarysupercapacitors");
        assertEquals(military, HighGradeEmissions.materialSymbols("Independent", List.of("War"), POPULATED));
        assertEquals(military, HighGradeEmissions.materialSymbols("Independent", List.of("CivilWar"), POPULATED));
    }

    @Test
    @DisplayName("a system in both war states still offers the pair once")
    void warAndCivilWarDoNotDuplicate() {
        assertEquals(List.of("militarygradealloys", "militarysupercapacitors"),
                HighGradeEmissions.materialSymbols("Independent", List.of("War", "CivilWar"), POPULATED));
    }

    @Test
    @DisplayName("boom and expansion both offer the proto trio")
    void boomStates() {
        List<String> proto = List.of("protoheatradiators", "protolightalloys", "protoradiolicalloys");
        assertEquals(proto, HighGradeEmissions.materialSymbols("Independent", List.of("Boom"), POPULATED));
        assertEquals(proto, HighGradeEmissions.materialSymbols("Independent", List.of("Expansion"), POPULATED));
    }

    @Test
    @DisplayName("outbreak offers Pharmaceutical Isolators only above a million people")
    void outbreakNeedsPopulation() {
        assertEquals(List.of("pharmaceuticalisolators"),
                HighGradeEmissions.materialSymbols("Independent", List.of("Outbreak"), 1_000_001L));
        assertTrue(HighGradeEmissions.materialSymbols("Independent", List.of("Outbreak"), 1_000_000L).isEmpty());
        assertTrue(HighGradeEmissions.materialSymbols("Independent", List.of("Outbreak"), 0L).isEmpty());
    }

    @Test
    @DisplayName("allegiance and state rules stack rather than competing")
    void rulesStack() {
        assertEquals(
                List.of("fedcorecomposites", "fedproprietarycomposites",
                        "protoheatradiators", "protolightalloys", "protoradiolicalloys"),
                HighGradeEmissions.materialSymbols("Federation", List.of("Boom"), POPULATED));
    }

    @Test
    @DisplayName("state spelling variants fold onto the same rule")
    void stateSpellingIsForgiving() {
        assertEquals(List.of("improvisedcomponents"),
                HighGradeEmissions.materialSymbols(null, List.of("civil unrest"), POPULATED));
        assertEquals(List.of("militarygradealloys", "militarysupercapacitors"),
                HighGradeEmissions.materialSymbols(null, List.of("Civil_War"), POPULATED));
        assertEquals(List.of("imperialshielding"),
                HighGradeEmissions.materialSymbols("  empire  ", List.of(), POPULATED));
    }

    @Test
    @DisplayName("states that mean nothing here are ignored, and nulls do not blow up")
    void irrelevantStatesAreIgnored() {
        assertTrue(HighGradeEmissions.materialSymbols(
                "Independent", java.util.Arrays.asList("Lockdown", "Famine", null, "  "), POPULATED).isEmpty());
    }
}
