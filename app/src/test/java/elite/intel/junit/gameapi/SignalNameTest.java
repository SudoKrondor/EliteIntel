package elite.intel.junit.gameapi;

import elite.intel.gameapi.SignalName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Every signal type a detailed surface scan reported across two months of journals, in the two shapes
 * Frontier writes them. The mining update added {@code $PlanetaryMiningLocation_Name;}, and the companion
 * read the symbol out loud: "we got a $SAA_SignalType_Geological signal and a $PlanetaryMiningLocation_Name
 * signal here".
 */
class SignalNameTest {

    @Test
    void aBodysSignalsAreNamedByTheGamesOwnTranslation() {
        assertEquals("Geological", SignalName.display("Geological", "$SAA_SignalType_Geological;"));
        assertEquals("Biological", SignalName.display("Biological", "$SAA_SignalType_Biological;"));
        assertEquals("Human", SignalName.display("Human", "$SAA_SignalType_Human;"));
        assertEquals("Planetary Mining Location",
                SignalName.display("Planetary Mining Location", "$PlanetaryMiningLocation_Name;"));
    }

    @Test
    void aSymbolWithNoTranslationIsTurnedBackIntoWords() {
        // Never the symbol itself, whatever the game does or does not send beside it.
        assertEquals("Geological", SignalName.display(null, "$SAA_SignalType_Geological;"));
        assertEquals("Planetary Mining Location", SignalName.display(null, "$PlanetaryMiningLocation_Name;"));
        assertEquals("Planetary Mining Location", SignalName.display("  ", "$PlanetaryMiningLocation_Name;"));
    }

    /**
     * A ring reports bare commodity names, and twelve of them carry no translation because none is needed.
     */
    @Test
    void aRingsReservesKeepTheirOwnNames() {
        assertEquals("Alexandrite", SignalName.display(null, "Alexandrite"));
        assertEquals("Painite", SignalName.display(null, "Painite"));
        assertEquals("tritium", SignalName.display(null, "tritium"));
        assertEquals("Low Temp. Diamonds", SignalName.display("Low Temp. Diamonds", "LowTemperatureDiamond"));
        assertEquals("Void Opal", SignalName.display("Void Opal", "Opal"));
        // Only when the game sends no wording of its own does the camel case get broken up.
        assertEquals("Low Temperature Diamond", SignalName.display(null, "LowTemperatureDiamond"));
    }

    @Test
    void nothingToNameStaysNothing() {
        assertNull(SignalName.display(null, null));
        assertNull(SignalName.display(null, "   "));
        assertNull(SignalName.display("", "$;"));
    }
}
