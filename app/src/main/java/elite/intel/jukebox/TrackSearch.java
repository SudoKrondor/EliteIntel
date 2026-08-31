package elite.intel.jukebox;

import elite.intel.db.dao.JukeboxDao;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Finds the track a commander just named out loud.
 * <p>
 * Speech recognition does not hand over song titles cleanly: punctuation vanishes, "and" becomes "&amp;" or
 * the reverse, and a title is as often remembered loosely as quoted exactly. So this scores on the words
 * that survive all of that rather than demanding the string back.
 * <p>
 * It deliberately answers nothing rather than a poor guess. Playing the wrong track is worse than saying
 * the title was not recognised, because the commander then has to work out what happened and undo it.
 */
public final class TrackSearch {

    /**
     * The share of the spoken words that must appear in a track before it can be the answer. Two thirds
     * tolerates a dropped article or a mangled word without letting one word in common carry a match.
     */
    private static final double MINIMUM_WORD_OVERLAP = 0.67;

    private TrackSearch() {
    }

    /**
     * The best match for a spoken title, or empty when nothing is close enough to be worth playing.
     * <p>
     * Title and artist are both searched, so "play some Stellar Cartography" finds a track by that artist
     * even though it names no title at all.
     */
    public static Optional<JukeboxDao.Track> find(List<JukeboxDao.Track> playlist, String spoken) {
        List<String> wanted = words(spoken);
        if (playlist == null || playlist.isEmpty() || wanted.isEmpty()) return Optional.empty();

        JukeboxDao.Track best = null;
        double bestScore = 0;
        for (JukeboxDao.Track track : playlist) {
            double score = score(track, wanted);
            if (score > bestScore) {
                bestScore = score;
                best = track;
            }
        }
        return bestScore >= MINIMUM_WORD_OVERLAP ? Optional.ofNullable(best) : Optional.empty();
    }

    /**
     * How much of what the commander said this track accounts for, from 0 to slightly over 1 - an exact
     * title match scores above a track that merely contains the same words, so the closer of two candidates
     * sharing words wins.
     */
    private static double score(JukeboxDao.Track track, List<String> wanted) {
        List<String> haystack = words(track.displayTitle() + " " + orEmpty(track.getArtist()));
        if (haystack.isEmpty()) return 0;
        int matched = 0;
        for (String word : wanted) {
            if (haystack.contains(word)) matched++;
        }
        if (matched == 0) return 0;
        double overlap = matched / (double) wanted.size();
        boolean titleSaidInFull = words(track.displayTitle()).equals(wanted);
        return titleSaidInFull ? overlap + 1 : overlap;
    }

    /**
     * Lower-cased words, with punctuation and the noise of dictation stripped out.
     */
    private static List<String> words(String text) {
        if (text == null || text.isBlank()) return List.of();
        return List.of(text.toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{N}]+", " ")
                        .trim()
                        .split("\\s+")).stream()
                .filter(word -> !word.isEmpty())
                .toList();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
