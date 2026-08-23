package elite.intel.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pad table is looked up with the journal's internal ship name, because that is what every caller has.
 *
 * <p>It used to hold display names only, so {@code asp} - the journal's word for an Asp Explorer - missed and
 * fell through to the large-ship default. Nothing failed: the commander was simply shown large-pad stations
 * for a medium ship, on every station search there is, forever.
 */
class ShipPadSizesTest {

    @Test
    void theJournalsOwnShipNamesResolve() {
        assertEquals(ShipPadSizes.MEDIUM, ShipPadSizes.getPadSize("asp"));
        assertEquals(ShipPadSizes.MEDIUM, ShipPadSizes.getPadSize("krait_mkii"));
        assertEquals(ShipPadSizes.MEDIUM, ShipPadSizes.getPadSize("mandalay"));
        assertEquals(ShipPadSizes.SMALL, ShipPadSizes.getPadSize("diamondbackxl"));
        assertEquals(ShipPadSizes.LARGE, ShipPadSizes.getPadSize("federation_corvette"));
        assertEquals(ShipPadSizes.LARGE, ShipPadSizes.getPadSize("cutter"));
    }

    /**
     * The hulls added late enough that the pad table did not have them at all, so they took the large-ship
     * default: two of them are large anyway, and the Kestrel is not.
     */
    @Test
    void theNewestHullsAreInTheTableRatherThanTakingTheDefault() {
        assertEquals(ShipPadSizes.LARGE, ShipPadSizes.getPadSize("explorer_nx"));
        assertEquals(ShipPadSizes.SMALL, ShipPadSizes.getPadSize("smallcombat01_nx"));
        assertEquals(ShipPadSizes.LARGE, ShipPadSizes.getPadSize("mediumtransport01"));
    }

    @Test
    void displayNamesStillResolveForACallerHoldingOne() {
        assertEquals(ShipPadSizes.MEDIUM, ShipPadSizes.getPadSize("Asp Explorer"));
        assertEquals(ShipPadSizes.LARGE, ShipPadSizes.getPadSize("Imperial Cutter"));
    }

    /**
     * A hull neither spelling knows is treated as large: fewer nearby options, but never a pad it cannot
     * land on.
     */
    @Test
    void anUnknownShipIsAssumedLarge() {
        assertEquals(ShipPadSizes.LARGE, ShipPadSizes.getPadSize("some_hull_released_next_year"));
        assertEquals(ShipPadSizes.LARGE, ShipPadSizes.getPadSize(null));
    }

    @Test
    void aLargeShipNeedsALargePadAndNothingElseWillDo() {
        assertTrue(ShipPadSizes.canDock(ShipPadSizes.LARGE, 0, 0, 1));
        assertFalse(ShipPadSizes.canDock(ShipPadSizes.LARGE, 8, 8, 0));
    }

    /**
     * Measured live against Spansh: stations with a large pad and no medium pad really do exist, so a medium
     * ship has to be allowed onto the large one rather than judged on the medium count alone.
     */
    @Test
    void aMediumShipTakesAMediumPadOrALargeOne() {
        assertTrue(ShipPadSizes.canDock(ShipPadSizes.MEDIUM, 0, 2, 0));
        assertTrue(ShipPadSizes.canDock(ShipPadSizes.MEDIUM, 2, 0, 1));
        assertFalse(ShipPadSizes.canDock(ShipPadSizes.MEDIUM, 4, 0, 0));
    }

    @Test
    void aSmallShipFitsAnyPadThereIs() {
        assertTrue(ShipPadSizes.canDock(ShipPadSizes.SMALL, 2, 0, 0));
        assertTrue(ShipPadSizes.canDock(ShipPadSizes.SMALL, 0, 1, 0));
        assertTrue(ShipPadSizes.canDock(ShipPadSizes.SMALL, 0, 0, 1));
        assertFalse(ShipPadSizes.canDock(ShipPadSizes.SMALL, 0, 0, 0));
    }
}
