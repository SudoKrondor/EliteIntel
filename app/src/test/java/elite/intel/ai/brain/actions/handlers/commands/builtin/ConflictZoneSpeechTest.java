package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.gameapi.signals.ConflictZoneProfile;
import elite.intel.gameapi.signals.WarZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What VEGA says about a war zone: the zones hardest first, and the sides only once an arrival has named
 * them.
 */
class ConflictZoneSpeechTest {

    @Test
    void theZonesAreReadHardestFirstAndTheSidesFollow() {
        WarZone ceos = new WarZone("Ceos", new ConflictZoneProfile(4, 0, 2, 0), "war", "Sirius Corporation", "RSR", 12.5);

        assertEquals("Ceos holds 2 high intensity, 4 low intensity conflict zones. A war: Sirius Corporation against RSR.",
                ConflictZoneSpeech.zones(ceos));
    }

    @Test
    void aCivilWarIsCalledOne() {
        WarZone zone = new WarZone("Sothis", new ConflictZoneProfile(0, 1, 0, 0), "civilwar", "A", "B", 0);

        assertEquals("Sothis holds 1 medium intensity conflict zones. A civil war: A against B.",
                ConflictZoneSpeech.zones(zone));
    }

    @Test
    void unnamedSidesAreLeftOut() {
        WarZone zone = new WarZone("Sothis", new ConflictZoneProfile(3, 0, 0, 0), null, null, null, 0);

        assertEquals("Sothis holds 3 low intensity conflict zones.", ConflictZoneSpeech.zones(zone));
    }

    @Test
    void powerplayZonesAreNotReadAloud() {
        WarZone zone = new WarZone("Sothis", new ConflictZoneProfile(1, 0, 0, 5), null, null, null, 0);

        assertEquals("Sothis holds 1 low intensity conflict zones.", ConflictZoneSpeech.zones(zone));
    }
}
