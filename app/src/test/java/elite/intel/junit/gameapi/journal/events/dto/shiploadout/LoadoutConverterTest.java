package elite.intel.junit.gameapi.journal.events.dto.shiploadout;

import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoadoutConverterTest {

    @Test
    void usesTrimmedShipNameWhenPresent() {
        assertEquals("My Ship", LoadoutConverter.toDisplayShipName("  My Ship  ", "mandalay"));
    }

    @Test
    void fallsBackToCapitalizedShipWhenShipNameIsBlank() {
        assertEquals("Mandalay", LoadoutConverter.toDisplayShipName("", "mandalay"));
        assertEquals("Mandalay", LoadoutConverter.toDisplayShipName("   ", "mandalay"));
    }

    @Test
    void fallsBackToCapitalizedTrimmedShipWhenShipNameIsMissing() {
        assertEquals("Mandalay", LoadoutConverter.toDisplayShipName(null, "  mandalay  "));
    }

    @Test
    void returnsNullForUnknownShipFallbackWhenShipNameAndShipAreBlank() {
        assertNull(
                LoadoutConverter.toDisplayShipName(null, null),
                "Unknown ship fallback should remain available when both names are missing"
        );
        assertNull(
                LoadoutConverter.toDisplayShipName("", "   "),
                "Unknown ship fallback should remain available when both names are blank"
        );
    }
}
