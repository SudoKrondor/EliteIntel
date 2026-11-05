package elite.intel.ui.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudAxisMeterTest {

    private static final int LEFT = 3;
    private static final int WIDTH = 101;

    @Test
    void centreIsTheMiddleOfTheTrackAndTheEndsAreItsEnds() {
        assertEquals(53, HudAxisMeter.position(0f, LEFT, WIDTH));
        assertEquals(3, HudAxisMeter.position(-1f, LEFT, WIDTH));
        assertEquals(103, HudAxisMeter.position(1f, LEFT, WIDTH));
    }

    @Test
    void movesBothWaysFromCentre() {
        assertEquals(28, HudAxisMeter.position(-0.5f, LEFT, WIDTH));
        assertEquals(78, HudAxisMeter.position(0.5f, LEFT, WIDTH));
    }

    @Test
    void aValueOutOfRangeStopsAtTheEnd() {
        assertEquals(3, HudAxisMeter.position(-1.5f, LEFT, WIDTH));
        assertEquals(103, HudAxisMeter.position(1.5f, LEFT, WIDTH));
    }
}
