package elite.intel.util;

import elite.intel.gameapi.search.edsm.dto.data.BodyData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The calculator takes a radius in METRES. EDSM publishes radii in kilometres, and feeding those in raw stored every
 * EDSM-learned body at a thousandth of its radius and a millionth-inverse of its gravity (see migration 01041).
 */
class GravityCalculatorTest {

    @Test
    void computesEarthGravitiesFromAMetreRadius() {
        // Earth itself: 1 Earth mass at 6371 km must come back as 1g.
        assertEquals(1.0, GravityCalculator.calculateSurfaceGravity(1.0, 6_371_000.0), 0.01);
    }

    @Test
    void aKilometreRadiusInflatesGravityByAMillion() {
        double metres = GravityCalculator.calculateSurfaceGravity(0.513865, 4_977_078.5);
        double kilometres = GravityCalculator.calculateSurfaceGravity(0.513865, 4977.0785);
        assertEquals(0.84, metres, 0.01);
        // The result is rounded to two decimals, so compare the ratio rather than the products.
        assertEquals(1_000_000.0, kilometres / metres, 10_000.0);
        assertFalse(GravityCalculator.isPlausible(kilometres), "this is what the table was full of");
    }

    @Test
    void edsmRadiusIsConvertedToMetres() {
        BodyData body = new BodyData();
        body.radius = 4977.0785; // EDSM publishes kilometres
        body.earthMasses = 0.513865;

        assertEquals(4_977_078.5, body.getRadiusMeters(), 0.001);
        assertEquals(0.84, GravityCalculator.calculateSurfaceGravity(body.getEarthMasses(), body.getRadiusMeters()), 0.01);
    }

    @Test
    void returnsNullForMissingMassOrRadius() {
        assertNull(GravityCalculator.calculateSurfaceGravity(0, 6_371_000.0));
        assertNull(GravityCalculator.calculateSurfaceGravity(1.0, 0));
    }

    @Test
    void rejectsGravitiesThatAreNotGravities() {
        assertTrue(GravityCalculator.isPlausible(0.35));
        assertTrue(GravityCalculator.isPlausible(199.31)); // Colonia 4, a real class III gas giant
        assertFalse(GravityCalculator.isPlausible(0), "zero is the not-recorded value");
        assertFalse(GravityCalculator.isPlausible(-1));
        assertFalse(GravityCalculator.isPlausible(355396.56), "the km-radius unit fault");
    }
}
