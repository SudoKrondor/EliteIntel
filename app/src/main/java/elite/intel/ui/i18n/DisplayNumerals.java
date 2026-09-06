package elite.intel.ui.i18n;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import elite.intel.util.NumberWords;

import java.text.ParsePosition;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puts the digits back into a line the commander <em>reads</em>, after it was spelled out for the voice.
 * <p>
 * Every figure the app speaks is spelled out in words before it reaches the TTS engine, because a number
 * handed over as digits is read through the engine's own locale and a group separator comes out as a decimal
 * point (see {@link elite.intel.util.NumberWords}). The chat log and the HUD overlay show that same sentence,
 * where the reverse is true: "one thousand two hundred twenty-four tonnes" is harder to take in at a glance
 * than "1,224 tonnes", and the commander asked for the figure they can read.
 * <p>
 * <b>Reading the words back.</b> The same CLDR rules that spell a number out will parse it back in, so this
 * needs no word list of its own and holds in all nine languages, including the forms a hand-written table
 * would miss: the compounds German and Italian run together, the twenties French counts in, the conjunctions
 * Portuguese puts between the parts, and the colloquial "twelve hundred twenty-four" the model may write
 * instead of the "one thousand two hundred twenty-four" it was given.
 * <p>
 * <b>What is left alone.</b> Spelled numbers are ordinary words as well as figures, so only a span that is
 * unambiguously a figure is converted:
 * <ul>
 *   <li>Values below {@value #SMALLEST_CONVERTED} stay words. "one of the systems", "no one answered" and
 *       "the second station" all parse as numbers and none of them is one; a threshold costs nothing but the
 *       small counts, which read perfectly well as words anyway - "four hours ago" is not improved by "4".</li>
 *   <li>A span that ends inside a word is dropped: "onerous" begins with "one" and "often" contains "ten".</li>
 *   <li>A run of digits is never touched, so a system name keeps its shape - Elite is full of them
 *       ("Col 285 Sector XY-Z c12-34"), and grouping the numbers inside one would corrupt it.</li>
 * </ul>
 * A fractional value is always converted whatever its size, because "one point zero two billion" is a figure
 * in any context, and it is how every large credit amount is spoken.
 * <p>
 * Display only. Nothing here reaches a TTS engine: the spoken text keeps its words.
 */
public final class DisplayNumerals {

    /**
     * Below this, a spelled number stays spelled. See the class notes: this is what keeps ordinary prose out
     * of the conversion.
     */
    static final int SMALLEST_CONVERTED = 100;

    /**
     * Parsing a rule set is not free, so one reader per language is built and shared.
     * {@link RuleBasedNumberFormat} is not thread safe and lines arrive on whatever thread the mouth is
     * running, so every read holds the instance's own lock.
     */
    private static final Map<Language, RuleBasedNumberFormat> READERS = new ConcurrentHashMap<>();

    /**
     * How many leading letters of a word are enough to tell it might begin a number. Three, so that the
     * languages that run a number into one word are still caught: German's "eintausendzweihundert..." has to
     * be recognised from "ein" even though the word for 1 on its own is "eins".
     */
    private static final int STEM = 3;

    /**
     * The stems any spelled number can begin with, per language, learned from the speller rather than listed
     * by hand. This is a filter, not a reader: it only decides whether a word is worth handing to ICU, and
     * a word that gets through and turns out to be prose is rejected there. Without it every word of every
     * line goes through a full parse, which on a long line costs seconds.
     */
    private static final Map<Language, Set<String>> NUMBER_STEMS = new ConcurrentHashMap<>();

    /**
     * Values whose spelling covers every word a number can start with: the units and teens, the tens, each
     * scale word, and a negative for languages that lead with their word for minus.
     */
    private static final long[] STEM_SEEDS = {
            -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 30, 40, 50, 60, 70, 80, 90, 100, 1_000, 1_000_000, 1_000_000_000L};

    private DisplayNumerals() {
    }

    /**
     * The line as the commander should read it, in the language it was composed in.
     * <p>
     * Not simply the session language: a Russian commander on the local voice, which cannot pronounce
     * Cyrillic, is answered in English, so the words to read back are English ones. The same policy decides
     * the language the figures were spelled in, which is what keeps the two halves in step.
     */
    public static String digits(String text) {
        return digits(text, AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance()));
    }

    static String digits(String text, Language language) {
        if (text == null || text.length() < 3) return text;
        RuleBasedNumberFormat reader = readerFor(language);
        StringBuilder out = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            Figure figure = startsAWord(text, index) && couldBeANumber(text, index, language)
                    ? readFigureAt(text, index, reader)
                    : null;
            if (figure == null) {
                out.append(text.charAt(index++));
                continue;
            }
            out.append(figure.formatted(language));
            index = figure.end();
        }
        return out.toString();
    }

    /**
     * A spelled number found in the text: what it is worth, and where the words that spell it end.
     */
    private record Figure(Number value, int end) {

        String formatted(Language language) {
            double number = value.doubleValue();
            return number == Math.rint(number)
                    ? LocalizedNumbers.grouped((long) number, language)
                    : LocalizedNumbers.decimal(number, language);
        }
    }

    private static boolean startsAWord(String text, int index) {
        return Character.isLetter(text.charAt(index))            // digits keep whatever shape they came in
                && (index == 0 || !Character.isLetter(text.charAt(index - 1)));
    }

    /**
     * Whether the word at this position is worth a parse. Cheap and deliberately generous: it lets through
     * "one" in "onerous" and "ten" in "tenders", which the parse then rejects.
     */
    private static boolean couldBeANumber(String text, int start, Language language) {
        int end = start;
        while (end < text.length() && end - start < STEM && Character.isLetter(text.charAt(end))) end++;
        return numberStems(language).contains(text.substring(start, end).toLowerCase(Locale.ROOT));
    }

    private static Set<String> numberStems(Language language) {
        return NUMBER_STEMS.computeIfAbsent(language, key -> {
            Set<String> stems = new HashSet<>();
            for (long seed : STEM_SEEDS) {
                String spelled = NumberWords.of(seed, key).toLowerCase(Locale.ROOT);
                int end = 0;
                while (end < spelled.length() && end < STEM && Character.isLetter(spelled.charAt(end))) end++;
                if (end > 0) stems.add(spelled.substring(0, end));
            }
            return Set.copyOf(stems);
        });
    }

    private static Figure readFigureAt(String text, int start, RuleBasedNumberFormat reader) {
        ParsePosition position = new ParsePosition(start);
        Number value;
        synchronized (reader) {
            value = reader.parse(text, position);
        }
        int end = position.getIndex();
        // ICU eats the space after the last word it read ("one point zero two " before "billion").
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) end--;
        if (value == null || end <= start) return null;
        if (end < text.length() && Character.isLetter(text.charAt(end))) return null; // "onerous", "often"

        double number = value.doubleValue();
        boolean whole = number == Math.rint(number);
        if (whole && Math.abs(number) < SMALLEST_CONVERTED) return null;

        return new Figure(value, end);
    }

    private static RuleBasedNumberFormat readerFor(Language language) {
        return READERS.computeIfAbsent(language, key -> {
            RuleBasedNumberFormat reader = new RuleBasedNumberFormat(
                    new ULocale(LocalizedNumbers.locale(key).toLanguageTag()), RuleBasedNumberFormat.SPELLOUT);
            // Lenient mode is not optional, however much it costs: strict parsing reads back only one value in
            // seven in German and two in seven in Italian, because it will not walk into a compound - it takes
            // "ein" out of "einhundert" and stops. It matches through a collator, which is why a word costs
            // about a millisecond and why nothing reaches it that the stem filter can rule out first.
            reader.setLenientParseMode(true);
            return reader;
        });
    }
}
