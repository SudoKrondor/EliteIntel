package elite.intel.ai.mouth.sherpa;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClauseChunkerTest {

    /**
     * The reported announcement at Beaufoy Horizons, one sentence of the LLM's. The session log cut it off
     * after "mission", so the list's tail here is the rest of the station's services in journal order.
     */
    private static final String THE_STATION = "Admiral, Beaufoy Horizons is a Federation colony with democratic"
            + " governance and control by the New Kachiri Progressive Party; services include dock, autodock,"
            + " blackmarket, commodities, contacts, exploration, missions, outfitting, crewlounge, rearm, refuel,"
            + " repair, tuning, engineer, missionsgenerated, flightcontroller, stationoperations and powerplay.";

    @Test
    void anOrdinarySentenceStaysWhole() {
        String sentence = "Surface Gravity registers at 0.22G, within operational tolerance.";
        assertEquals(List.of(sentence), ClauseChunker.chunk(sentence));
    }

    @Test
    void theStationIsQueuedInPiecesThatSayTheSameThing() {
        List<String> pieces = ClauseChunker.chunk(THE_STATION);

        assertTrue(pieces.size() > 1, "the paragraph-long sentence must be cut");
        assertEquals(THE_STATION, String.join(" ", pieces), "cutting must not change a word");
    }

    @Test
    void theFirstPieceIsShortSoTheVoiceStartsSoon() {
        // The faction clause has no comma in it, so it stays whole; the lone "Admiral," joins it.
        assertEquals("Admiral, Beaufoy Horizons is a Federation colony with democratic governance and control"
                + " by the New Kachiri Progressive Party;", ClauseChunker.chunk(THE_STATION).getFirst());
    }

    @Test
    void theListIsPackedRatherThanReadOneWordAtATime() {
        List<String> pieces = ClauseChunker.chunk(THE_STATION);
        for (String piece : pieces.subList(1, pieces.size())) {
            assertTrue(piece.length() >= ClauseChunker.MIN_PIECE, "too short: " + piece);
            assertTrue(piece.length() <= ClauseChunker.TARGET_PIECE, "too long: " + piece);
        }
    }

    @Test
    void aNumberIsNeverCutAtItsComma() {
        String sentence = "The bounty on this pilot comes to 1,250,000 credits and the fine to 0,22 percent of"
                + " it, which the authority will collect at the next station you dock at today.";
        for (String piece : ClauseChunker.chunk(sentence)) {
            assertTrue(!piece.startsWith("250") && !piece.startsWith("000") && !piece.startsWith("22"), piece);
        }
    }

    @Test
    void aLongClauseWithNoPunctuationIsNeverCut() {
        String sentence = "This is one long clause that goes on and on without a single comma or semicolon"
                + " anywhere in it because the model decided to write it that way today.";
        assertEquals(List.of(sentence), ClauseChunker.chunk(sentence));
    }

    @Test
    void aShortTailJoinsThePieceBeforeIt() {
        String sentence = "Refuel, repair and rearm are offered here at the commodities desk and the outfitting"
                + " counter on the main concourse level, as usual.";
        List<String> pieces = ClauseChunker.chunk(sentence);
        assertTrue(pieces.getLast().length() >= ClauseChunker.MIN_PIECE, pieces.toString());
    }
}
