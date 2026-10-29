package elite.intel.ai.ears;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Recognises transcripts that are nothing but laughter.
 * <p>
 * Laughter is what the STT engine reaches for when it is handed a burst of
 * non-speech noise it cannot map to words - a cough, a chair, a knock on the
 * desk all come back as "ha ha ha". The commander never said anything, so the
 * transcript must not reach the AI: routing it costs a round trip and can even
 * be answered, which is worse than silence.
 * <p>
 * Kept separate from {@code trashPhrases()} because laughter is not a fixed
 * phrase - it is a syllable repeated an arbitrary number of times, spelled
 * apart ("ha ha ha") or together ("hahaha"), so a word list can never cover it.
 * The syllable itself is language-specific, though only just: across the nine
 * locales this app ships, an engine writes laughter with {@code h} ("hahaha"),
 * with {@code j} in Spanish ("jajaja"), or with {@code х} in Cyrillic
 * ("хаха"), and those three cover it.
 */
public final class LaughterFilter {

    /**
     * The laughter syllable of each spelling: an aspirated vowel, optionally
     * closed ("hah"). The vowel set deliberately excludes {@code i} and
     * {@code o} so real short words ("hi", "ho") cannot combine into a match.
     */
    private static final List<String> SYLLABLES = List.of("h[aeu]h?", "j[aeu]h?", "х[аеу]х?");

    /**
     * Two syllables minimum, and both from the same spelling. A single "ha" is
     * too close to a real fragment to throw away on its own, and mixing the
     * alternatives would match across them: "haja" is a Portuguese word, and
     * laughter does not change consonant halfway through anyway.
     */
    private static final Pattern LAUGHTER = Pattern.compile(
            SYLLABLES.stream().map(syllable -> "(?:" + syllable + "){2,}")
                    .collect(Collectors.joining("|", "^(?:", ")$")));

    /**
     * Everything that is not a letter: spacing and punctuation carry no meaning
     * here, so "ha ha ha", "ha-ha-ha" and "hahaha!" all normalise to one form.
     */
    private static final Pattern NON_LETTER = Pattern.compile("[^\\p{L}]+");

    private LaughterFilter() {
    }

    /**
     * @param transcript raw STT output, may be null or blank
     * @return true if the transcript consists only of laughter and should be discarded
     */
    public static boolean isLaughter(String transcript) {
        if (transcript == null || transcript.isBlank()) return false;
        String letters = NON_LETTER.matcher(transcript).replaceAll("").toLowerCase(Locale.ROOT);
        if (letters.isEmpty()) return false;
        return LAUGHTER.matcher(letters).matches();
    }
}
