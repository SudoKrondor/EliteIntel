package elite.intel.vega;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanionAddressingTest {

    @Test
    void stripsOneLeadingConfiguredNameWithoutTouchingTheCommand() {
        String name = CompanionConfig.companionNameForms().getFirst();

        assertEquals("remember the docking code",
                CompanionAddressing.stripLeadingName(name + ", remember the docking code"));
        assertEquals("remember the docking code",
                CompanionAddressing.stripLeadingName(name + " remember the docking code"));
    }

    @Test
    void leavesBareOrEmbeddedNameUntouched() {
        String name = CompanionConfig.companionNameForms().getFirst();

        assertEquals(name, CompanionAddressing.stripLeadingName(name));
        assertEquals("tell " + name + " to wait", CompanionAddressing.stripLeadingName("tell " + name + " to wait"));
    }
}
