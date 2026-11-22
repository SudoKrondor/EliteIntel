package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.gameapi.search.spansh.station.shipyard.ShipyardCatalogue;
import elite.intel.gameapi.search.spansh.station.shipyard.ShipyardShip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The hull a commander named, out of the ways a ship name actually gets said and transcribed. Run against the
 * real catalogue with no client names, so no database is needed.
 */
class SpokenShipTest {

    private static final Function<ShipyardShip, String> NO_CLIENT_NAMES = ship -> null;

    @ParameterizedTest
    @CsvSource({
            "cobra mk iii,         CobraMkIII",
            "Cobra MkIII,          CobraMkIII",
            "cobra mark 3,         CobraMkIII",
            "cobra mark three,     CobraMkIII",
            "cobra mk. 5,          CobraMkV",
            "python,               Python",
            "python mk 2,          Python_NX",
            "anaconda,             Anaconda",
            "anaconda mark two,    AnacondaMkII",
            "imperial cutter,      Cutter",
            "cutter,               Cutter",
            "corvette,             Federation_Corvette",
            "clipper,              Empire_Trader",
            "eagle,                Eagle",
            "imperial eagle,       Empire_Eagle",
            "type 9,               Type9",
            "type nine,            Type9",
            "type-10,              Type9_Military",
            "fer de lance,         FerDeLance",
            "a fer-de-lance ship,  FerDeLance",
            "assault ship,         Federation_Dropship_MkII",
            "beluga,               BelugaLiner",
            "pyton,                Python",
            "anakonda,             Anaconda",
            "kestrel,              SmallCombat01_NX",
    })
    void aSpokenNameFindsItsHull(String spoken, String symbol) {
        SpokenShip.Match match = SpokenShip.resolve(spoken, ShipyardCatalogue.all(), NO_CLIENT_NAMES);

        SpokenShip.One one = assertInstanceOf(SpokenShip.One.class, match, spoken);
        assertEquals(symbol, one.ship().symbol());
    }

    /**
     * A family name is several hulls, and the commander is asked rather than sent to whichever is nearest.
     */
    @Test
    void aFamilyNameIsAskedAbout() {
        SpokenShip.Several several = assertInstanceOf(SpokenShip.Several.class,
                SpokenShip.resolve("cobra", ShipyardCatalogue.all(), NO_CLIENT_NAMES));

        assertEquals(List.of("Cobra MkIII", "Cobra MkIV", "Cobra MkV"),
                several.ships().stream().map(ShipyardShip::name).toList());
    }

    @Test
    void aWordThatIsNoShipIsNone() {
        assertInstanceOf(SpokenShip.None.class, SpokenShip.resolve("tritium", ShipyardCatalogue.all(), NO_CLIENT_NAMES));
        assertInstanceOf(SpokenShip.None.class, SpokenShip.resolve("", ShipyardCatalogue.all(), NO_CLIENT_NAMES));
    }

    /**
     * The largest real family - the six Lakon Types - is still asked about, all six named.
     */
    @Test
    void theWholeTypeFamilyIsAskedAbout() {
        SpokenShip.Several several = assertInstanceOf(SpokenShip.Several.class,
                SpokenShip.resolve("type", ShipyardCatalogue.all(), NO_CLIENT_NAMES));

        assertEquals(SpokenShip.MAX_CHOICES, several.ships().size());
    }

    /**
     * The game client's own name for a hull is heard too, which on a localized client is the one he reads.
     */
    @Test
    void theClientNameIsHeard() {
        Function<ShipyardShip, String> russian = ship -> ship.symbol().equals("Empire_Trader") ? "Имперский клипер" : null;

        SpokenShip.One one = assertInstanceOf(SpokenShip.One.class,
                SpokenShip.resolve("имперский клипер", ShipyardCatalogue.all(), russian));
        assertEquals("Empire_Trader", one.ship().symbol());
    }
}
