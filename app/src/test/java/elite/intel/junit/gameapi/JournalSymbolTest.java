package elite.intel.junit.gameapi;

import elite.intel.gameapi.JournalSymbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JournalSymbolTest {

    @Test
    @DisplayName("all three of Frontier's spellings normalise to the same symbol")
    void spellingsConverge() {
        // mission Commodity, MarketBuy Type, and a cargo hold row for the same good
        assertEquals("advancedmedicines", JournalSymbol.normalize("$AdvancedMedicines_Name;"));
        assertEquals("advancedmedicines", JournalSymbol.normalize("advancedmedicines"));
        assertEquals("hazardousenvironmentsuits", JournalSymbol.normalize("$HazardousEnvironmentSuits_Name;"));
    }

    @Test
    @DisplayName("nothing to normalise stays nothing")
    void handlesAbsentValues() {
        assertNull(JournalSymbol.normalize(null));
        assertNull(JournalSymbol.normalize("   "));
        assertNull(JournalSymbol.normalize("$_Name;"));
    }
}
