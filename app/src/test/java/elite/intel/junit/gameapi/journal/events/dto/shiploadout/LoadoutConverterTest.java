package elite.intel.junit.gameapi.journal.events.dto.shiploadout;

import elite.intel.db.managers.ShipMakeManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoadoutConverterTest {

    @Test
    void usesTrimmedShipNameWhenPresent() {
        assertEquals("My Ship", LoadoutConverter.toDisplayShipName("  My Ship  ", "smallcombat01_nx"));
    }

    @Test
    void playerAssignedNameTakesPrecedenceOverTableLookup() {
        assertEquals("Iron Cobra", LoadoutConverter.toDisplayShipName("Iron Cobra", "cobramkiii"));
    }

    @Test
    void resolvesSeededShipTypeToDisplayName() {
        assertEquals("Kestrel Mk II", LoadoutConverter.toDisplayShipName(null, "smallcombat01_nx"));
        assertEquals("Type-10 Defender", LoadoutConverter.toDisplayShipName(null, "type9_military"));
        assertEquals("Asp Explorer", LoadoutConverter.toDisplayShipName(null, "asp"));
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertEquals("Kestrel Mk II", LoadoutConverter.toDisplayShipName(null, "SmallCombat01_NX"));
        assertEquals("Federal Corvette", LoadoutConverter.toDisplayShipName(null, "Federation_Corvette"));
    }

    @Test
    void fallsBackToTitleCaseForShipTypeNotInTable() {
        assertEquals("Somenewship", LoadoutConverter.toDisplayShipName(null, "somenewship"));
    }

    @Test
    void trimsWhitespaceBeforeLookup() {
        assertEquals("Kestrel Mk II", LoadoutConverter.toDisplayShipName(null, "  smallcombat01_nx  "));
    }

    @Test
    void upsertedDisplayNameIsVisibleWithinSameSession() {
        ShipMakeManager.getInstance().upsert("futureShip_TrX", "Future Ship TrX");
        assertEquals("Future Ship TrX", LoadoutConverter.toDisplayShipName(null, "futureship_trx"));
    }

    @Test
    void returnsNullWhenBothNamesAreMissing() {
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
