package elite.intel.ui.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThemeRoleTest {

    /**
     * Every theme file is a list of wire names, so each role must be found by the name it writes. Two roles sharing
     * a name fail here too: the lookup table refuses a duplicate when the enum loads.
     */
    @Test
    void everyRoleIsFoundByItsWireName() {
        for (ThemeRole role : ThemeRole.values()) {
            assertEquals(role, ThemeRole.byWireName(role.wireName()).orElseThrow());
        }
    }
}
