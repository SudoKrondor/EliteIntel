package elite.intel.util;

import elite.intel.i18n.Language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Strips conversational filler / discourse openers that the LLM prepends despite the
 * "never open with filler words" instruction in
 * {@link elite.intel.ai.brain.ShipPersonality}. The prompt rule only reduces frequency;
 * this is the deterministic, locale-aware backstop applied to every TTS string via
 * {@link StringUtls#sanitizeTts(String)}.
 * <p>
 * Two classes of opener are handled per language:
 * <ul>
 *   <li><b>Interjections</b> ({@code oh}, {@code ah}, {@code hmm}, …) — stripped with or
 *       without a trailing comma. These carry no meaning as a sentence opener.</li>
 *   <li><b>Discourse markers</b> ({@code so}, {@code now}, {@code well}, …) — stripped
 *       <em>only</em> when followed by a comma/period, so a legitimate sentence such as
 *       "So many contacts detected" survives while "So, plotting course" is trimmed.</li>
 * </ul>
 * Matching is case-insensitive (Unicode-aware, so it works for Cyrillic/accented scripts),
 * tolerant of stacked openers ("Ah, well, now, …"), re-capitalizes the surviving first
 * letter, and never strips a message down to empty.
 * <p>
 * <b>Localizers:</b> edit only the two lists for your language below. An empty list means
 * "no stripping" for that class. Word boundaries are enforced automatically, so list bare
 * words ({@code "so"}, not {@code "so,"}).
 */
public final class TtsFillerRules {

    /**
     * Separators that may follow/consume after an opener: whitespace, comma, period, bang, ellipsis.
     */
    private static final String SEP = "[\\s,.!…]";

    private static final EnumMap<Language, Pattern> CACHE = new EnumMap<>(Language.class);

    private TtsFillerRules() {
    }

    /**
     * Removes leading filler/discourse openers for the given language.
     *
     * @param input the (already-produced) speech text; may be null/blank
     * @param lang  the session language whose filler lists to apply
     * @return the input with leading fillers removed and the first letter re-capitalized,
     * or the original (trimmed) text if removal would empty it or no rules apply
     */
    public static String stripLeading(String input, Language lang) {
        if (input == null) return "";
        if (input.isBlank()) return input;

        Pattern pattern = CACHE.computeIfAbsent(lang, TtsFillerRules::build);
        if (pattern == null) return input;

        String result = pattern.matcher(input).replaceFirst("").trim();
        if (result.isEmpty()) return input.trim(); // never strip the entire message

        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    private static Pattern build(Language lang) {
        List<String> interjections = new ArrayList<>(interjections(lang));
        List<String> markers = new ArrayList<>(markers(lang));
        if (interjections.isEmpty() && markers.isEmpty()) return null;

        // Longer phrases first so multi-word openers ("look at us") win over their prefixes.
        interjections.sort(Comparator.comparingInt(String::length).reversed());
        markers.sort(Comparator.comparingInt(String::length).reversed());

        List<String> alternatives = new ArrayList<>();
        // Interjection: must be followed by a separator or end-of-string (word boundary),
        // then greedily consume the trailing separators. Comma optional.
        for (String w : interjections) {
            alternatives.add(Pattern.quote(w) + "(?=" + SEP + "|$)" + SEP + "*");
        }
        // Discourse marker: REQUIRE at least one comma/period/bang after it (optionally
        // preceded by spaces). The required punctuation doubles as the word boundary.
        for (String w : markers) {
            alternatives.add(Pattern.quote(w) + "\\s*[,.!…]+\\s*");
        }

        // (?iu) = case-insensitive + Unicode-aware case folding.
        String regex = "(?iu)^\\s*(?:" + String.join("|", alternatives) + ")+";
        return Pattern.compile(regex);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-language filler lists. Bare words only — boundaries are handled above.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pure interjection openers — stripped with or without a trailing comma.
     */
    private static List<String> interjections(Language lang) {
        return switch (lang) {
            case EN ->
                    List.of("oh", "ohh", "ah", "ahh", "uh", "uhh", "um", "umm", "hmm", "hm", "er", "erm", "eh", "huh");
            case RU -> List.of("ох", "ах", "эх", "ой", "эй", "гм", "хм", "эм", "ну да");
            case UK -> List.of("ох", "ах", "ех", "ой", "гм", "хм", "ну да");
            case DE -> List.of("oh", "ach", "äh", "ähm", "hm", "hmm", "tja", "na ja", "naja");
            case FR -> List.of("oh", "ah", "euh", "heu", "hein", "ben", "bah", "hmm");
            case ES -> List.of("oh", "ah", "eh", "ehm", "mmm", "ay");
            case IT -> List.of("oh", "ah", "eh", "ehm", "mah", "mmm", "beh");
            case PT -> List.of("oh", "ah", "eh", "hum", "hmm");
        };
    }

    /**
     * Discourse-marker openers — stripped ONLY when followed by a comma/period.
     */
    private static List<String> markers(Language lang) {
        return switch (lang) {
            case EN -> List.of("well", "so", "now", "look", "look at us", "right", "okay", "ok",
                    "alright", "alrighty", "listen", "you know", "i mean", "basically", "actually", "anyway");
            case RU -> List.of("ну", "так", "вот", "итак", "значит", "короче", "в общем",
                    "слушай", "знаешь", "понимаешь");
            case UK -> List.of("ну", "так", "от", "отож", "отже", "значить", "коротше",
                    "слухай", "знаєш", "розумієш");
            case DE -> List.of("also", "nun", "na", "tja", "schau", "hör mal", "weißt du",
                    "eigentlich", "sozusagen", "gut");
            case FR -> List.of("eh bien", "bon", "bah", "alors", "donc", "écoute", "tu sais",
                    "en fait", "voilà", "bref");
            case ES -> List.of("bueno", "pues", "vale", "mira", "oye", "o sea", "en fin",
                    "a ver", "vamos", "es decir");
            case IT -> List.of("allora", "dunque", "beh", "senti", "guarda", "cioè",
                    "insomma", "ecco", "vabbè", "niente");
            case PT -> List.of("bem", "bom", "então", "olha", "escuta", "quer dizer",
                    "ou seja", "enfim", "pronto");
        };
    }
}
