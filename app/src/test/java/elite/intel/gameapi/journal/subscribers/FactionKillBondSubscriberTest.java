package elite.intel.gameapi.journal.subscribers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Which name of a faction goes into the combat-bond ledger, and from there onto the HUD card's SIDE row.
 */
class FactionKillBondSubscriberTest {

    @Test
    void anOrdinaryFactionIsFiledUnderItsOwnName() {
        assertEquals("Sirius Corporation", FactionKillBondSubscriber.preferPlain("Sirius Corporation", null));
    }

    @Test
    void aSymbolicFactionIsFiledUnderItsLocalisedName() {
        assertEquals("Pilots' Federation",
                FactionKillBondSubscriber.preferPlain("$faction_PilotsFederation;", "Pilots' Federation"));
    }

    @Test
    void aSymbolWithNoLocalisedTwinStaysASymbol() {
        assertEquals("$faction_Thargoid;", FactionKillBondSubscriber.preferPlain("$faction_Thargoid;", " "));
    }

    @Test
    void noNameAtAllStaysEmpty() {
        assertNull(FactionKillBondSubscriber.preferPlain(null, null));
    }
}
