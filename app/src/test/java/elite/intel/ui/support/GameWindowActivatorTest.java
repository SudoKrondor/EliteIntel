package elite.intel.ui.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameWindowActivatorTest {

    @Test
    void placesTheClientAreaAtTheTopOfItsMonitorWithoutChangingWindowSize() {
        assertEquals(-31, GameWindowActivator.topForHiddenCaption(0, 0, 31));
        assertEquals(1049, GameWindowActivator.topForHiddenCaption(1080, 1080, 1111));
        assertEquals(-1080, GameWindowActivator.topForHiddenCaption(-1080, -1080, -1080));
    }
}
