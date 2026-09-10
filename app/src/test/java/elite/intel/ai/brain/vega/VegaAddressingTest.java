package elite.intel.ai.brain.vega;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VegaAddressingTest {

    @Test
    void stripsOneLeadingConfiguredNameWithoutTouchingTheCommand() {
        String name = VegaConfig.vegaNameForms().getFirst();

        assertEquals("remember the docking code",
                VegaAddressing.stripLeadingName(name + ", remember the docking code"));
        assertEquals("remember the docking code",
                VegaAddressing.stripLeadingName(name + " remember the docking code"));
    }

    @Test
    void leavesBareOrEmbeddedNameUntouched() {
        String name = VegaConfig.vegaNameForms().getFirst();

        assertEquals(name, VegaAddressing.stripLeadingName(name));
        assertEquals("tell " + name + " to wait", VegaAddressing.stripLeadingName("tell " + name + " to wait"));
    }
}
