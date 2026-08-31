package elite.intel.jukebox;

import elite.intel.db.dao.JukeboxDao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finding the track a commander named out loud.
 *
 * <p>What arrives here has been through speech recognition, so it has lost its punctuation and may have
 * lost a word. What must never happen is playing the wrong thing: a near miss costs the commander the
 * trouble of working out what happened and undoing it, while "I cannot find that" costs them one repeat.
 */
class TrackSearchTest {

    private static final List<JukeboxDao.Track> PLAYLIST = List.of(
            track(1, "/m/aphelion.mp3", "Aphelion Drift", "Stellar Cartography"),
            track(2, "/m/perihelion.mp3", "Perihelion", "Stellar Cartography"),
            track(3, "/m/deep.mp3", "Deep Black", "Void Runners"),
            track(4, "/m/untitled.mp3", null, null));

    @Test
    void anExactTitleIsFound() {
        assertEquals(1, found("Aphelion Drift"));
    }

    @Test
    void caseAndPunctuationDoNotMatter() {
        assertEquals(3, found("deep black"));
        assertEquals(3, found("Deep, Black!"));
    }

    @Test
    void anArtistNameFindsTheirTrack() {
        assertTrue(TrackSearch.find(PLAYLIST, "Void Runners").isPresent(),
                "asking for an artist should find something of theirs, not nothing");
    }

    @Test
    void theCloserOfTwoSimilarTitlesWins() {
        // Both tracks are by the same artist and share a word, so the one whose title was actually said
        // has to win rather than whichever happened to be first in the list.
        assertEquals(2, found("Perihelion"));
    }

    @Test
    void aTitleWithNoTagsIsStillFindableByItsFileName() {
        assertEquals(4, found("untitled"),
                "the table shows the file name for an untagged track, so that is what gets asked for");
    }

    @Test
    void aTrackThatIsNotThereIsNotSubstitutedForSomethingElse() {
        assertTrue(TrackSearch.find(PLAYLIST, "Symphony of the Void").isEmpty(),
                "the wrong track is worse than none - the commander then has to undo it");
    }

    @Test
    void oneWordInCommonIsNotAMatch() {
        assertTrue(TrackSearch.find(PLAYLIST, "the black pearl adventure song").isEmpty(),
                "sharing a single word with a title must not be enough to start playing it");
    }

    @Test
    void nothingIsFoundInAnEmptyPlaylist() {
        assertTrue(TrackSearch.find(List.of(), "Aphelion Drift").isEmpty());
    }

    @Test
    void anEmptyRequestFindsNothingRatherThanTheFirstTrack() {
        assertTrue(TrackSearch.find(PLAYLIST, "").isEmpty());
        assertTrue(TrackSearch.find(PLAYLIST, null).isEmpty());
        assertTrue(TrackSearch.find(PLAYLIST, "   ").isEmpty());
    }

    private static long found(String spoken) {
        return TrackSearch.find(PLAYLIST, spoken)
                .orElseThrow(() -> new AssertionError("nothing matched: " + spoken))
                .getId();
    }

    private static JukeboxDao.Track track(long id, String path, String title, String artist) {
        JukeboxDao.Track track = new JukeboxDao.Track();
        track.setId(id);
        track.setPath(path);
        track.setTitle(title);
        track.setArtist(artist);
        return track;
    }
}
