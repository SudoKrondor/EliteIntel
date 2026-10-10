package elite.intel.gameapi.cartography;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The formula against what is known to be true: the community's median table, reproduced cell for cell, and
 * the commander's own sales that can be isolated to a known set of bodies.
 */
class CartographicValueTest {

    // -- the median table (MattG, Frontier forums, Odyssey) -------------------------------------------------
    // Columns: FSS / FSS+FD / FSS+DSS / FSS+DSS+FM / FSS+DSS+FD+FM, every mapping within the probe target.

    @Test
    void earthLikeWorld() {
        assertTable("Earthlike body", false, 0.498039, 270_290, 702_753, 1_464_068, 3_555_753, 4_224_870);
    }

    @Test
    void ammoniaWorld() {
        assertTable("Ammonia world", false, 0.43914, 143_463, 373_004, 777_091, 1_887_305, 2_242_455);
    }

    @Test
    void waterWorlds() {
        assertTable("Water world", false, 0.780638, 99_747, 259_343, 540_297, 1_312_209, 1_559_138);
        assertTable("Water world", true, 0.453011, 268_616, 698_400, 1_455_001, 3_533_732, 4_198_704);
    }

    @Test
    void highMetalContentAndMetalRich() {
        assertTable("High metal content body", false, 0.344919, 14_070, 36_581, 76_211, 185_092, 219_923);
        assertTable("High metal content body", true, 0.466929, 163_948, 426_264, 888_051, 2_156_792, 2_562_654);
        assertTable("Metal rich body", false, 0.323933, 31_632, 82_244, 171_342, 416_136, 494_443);
    }

    @Test
    void rockAndIce() {
        assertTable("Icy body", false, 0.01854, 500, 1_300, 2_262, 4_953, 6_330);
        assertTable("Rocky body", true, 0.142312, 129_504, 336_711, 701_482, 1_703_675, 2_024_270);
    }

    @Test
    void gasGiants() {
        assertTable("Sudarsky class I gas giant", false, 69.55164, 3_845, 9_997, 20_828, 50_584, 60_103);
        assertTable("Sudarsky class II gas giant", false, 476.2409, 28_405, 73_853, 153_861, 373_679, 443_997);
        assertTable("Sudarsky class III gas giant", false, 1148.922, 995, 2_587, 5_389, 13_088, 15_551);
    }

    @Test
    void aMappingOverTheProbeTargetLosesTheEfficiencyBonus() {
        // first mapped, not first discovered: the table's FSS+DSS+FM column, with and without the x1.25
        CartographicBody efficient = planet("Earthlike body", false, 0.498039, true, false).withMapping(true);
        CartographicBody wasteful = planet("Earthlike body", false, 0.498039, true, false).withMapping(false);

        assertEquals(3_555_753, CartographicValue.body(efficient));
        assertEquals(2_844_603, CartographicValue.body(wasteful));
    }

    @Test
    void aBubbleBodyChartedBeforeDiscoveryExistedPaysTheDiscovererButNotTheOdysseyShare() {
        CartographicBody body = new CartographicBody("Bubble 1", false, null, 0, "Earthlike body", false,
                0.498039, false, true, true, true);

        // FSS 270,290, mapped by someone else (x3.3333), efficient (x1.25), first discoverer (x2.6)
        assertEquals(2_928_136, CartographicValue.body(body));
    }

    // -- stars -----------------------------------------------------------------------------------------------

    @Test
    void starsArePricedByKindAndMass() {
        assertEquals(1_213, CartographicValue.body(star("K", 0.7, true)));
        assertEquals(23_106, CartographicValue.body(star("N", 1.4, true)));
        assertEquals(14_184, CartographicValue.body(star("DA", 0.6, true)));
        assertEquals(24_165, CartographicValue.body(star("H", 4.5, true)));
    }

    @Test
    void aFirstDiscoveredStarPaysTheDiscoverersMultiplier() {
        assertEquals(3_153, CartographicValue.body(star("K", 0.7, false)));
    }

    // -- systems: the commander's own sales ------------------------------------------------------------------

    @Test
    void aLoneStarHonkedInASystemOfOneSoldForItsOwnValue() {
        // 2026-08-07: Trianguli Sector YF-N a7-5, one T Tauri star, honk counted 1 body - BaseValue 1,201.
        assertEquals(1_201, CartographicValue.system(List.of(primary("TTS", 0.046875)), 1, true));
    }

    @Test
    void theHonkPricesEveryBodyNobodyScannedAtTheFloor() {
        // 2026-08-12: three honked systems, only the arrival star scanned in each - BaseValue 12,103.
        long piscium = CartographicValue.system(List.of(primary("Y", 0.023438)), 12, false);
        long tascheter = CartographicValue.system(List.of(primary("Y", 0.03125)), 1, true);
        long col359 = CartographicValue.system(List.of(primary("L", 0.132813)), 7, false);

        assertEquals(12_103, piscium + tascheter + col359);
    }

    @Test
    void anUnhonkedSystemIsWorthItsScansAlone() {
        // 2026-08-21: Col 285 Sector GG-Y a30-1, no honk, the star and one icy body autoscanned.
        long value = CartographicValue.system(List.of(
                primary("T", 0.089844),
                planet("Icy body", false, 0.027268, true, true)), null, false);

        assertEquals(1_202 + 500, value);
    }

    @Test
    void aScannedBodyAddsAThirdOfItsBaseToTheHonk() {
        CartographicBody gasGiant = planet("Sudarsky class II gas giant", false, 476.2409, true, true);

        long withHonk = CartographicValue.system(List.of(primary("G", 1.0), gasGiant), 2, false);
        long withoutHonk = CartographicValue.system(List.of(primary("G", 1.0), gasGiant), null, false);

        assertEquals(9_468, withHonk - withoutHonk, "a third of the gas giant's 28,405");
    }

    @Test
    void aFirstDiscoveryPaysTheFullScanBonusOnceTheFssIsDone() {
        CartographicBody ours = new CartographicBody("Ours", true, "M", 0.4, null, false, 0, false, false, false, false);

        long surveying = CartographicValue.system(List.of(ours), 5, false);
        long finished = CartographicValue.system(List.of(ours), 5, true);

        assertEquals(5_000, finished - surveying);
        assertEquals(CartographicValue.body(ours) + 4 * 1_300, surveying,
                "a first discovery prices its unscanned bodies at the floor times the discoverer's multiplier");
    }

    // -- fixtures --------------------------------------------------------------------------------------------

    private static void assertTable(String planetClass, boolean terraformable, double mass,
                                    long fss, long firstDiscovered, long mapped, long firstMapped, long both) {
        assertEquals(fss, CartographicValue.body(planet(planetClass, terraformable, mass, true, true)), "FSS");
        assertEquals(firstDiscovered, CartographicValue.body(planet(planetClass, terraformable, mass, false, true)), "FSS+FD");
        assertEquals(mapped, CartographicValue.body(planet(planetClass, terraformable, mass, true, true).withMapping(true)), "FSS+DSS");
        assertEquals(firstMapped, CartographicValue.body(planet(planetClass, terraformable, mass, true, false).withMapping(true)), "FSS+DSS+FM");
        assertEquals(both, CartographicValue.body(planet(planetClass, terraformable, mass, false, false).withMapping(true)), "FSS+DSS+FD+FM");
    }

    private static CartographicBody planet(String planetClass, boolean terraformable, double massEM,
                                           boolean wasDiscovered, boolean wasMapped) {
        return new CartographicBody("Test " + planetClass, false, null, 0, planetClass, terraformable, massEM,
                wasDiscovered, wasMapped, false, false);
    }

    private static CartographicBody star(String type, double mass, boolean wasDiscovered) {
        return new CartographicBody("Test star " + type, false, type, mass, null, false, 0, wasDiscovered, false,
                false, false);
    }

    private static CartographicBody primary(String type, double mass) {
        return new CartographicBody("Test primary " + type, true, type, mass, null, false, 0, true, false,
                false, false);
    }
}
