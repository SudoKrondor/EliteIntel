package elite.intel.ai.mouth.supertonic;

import elite.intel.i18n.Language;
import elite.intel.util.SignedAndDecimalNumbers;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Makes text unambiguous for Supertonic's multilingual tokenizer.
 */
final class SupertonicTextNormalizer {

    // Long uppercase words are emphasis, not acronyms: "RIATTIVA" must stay one word.
    private static final Pattern UPPERCASE_ACRONYM = Pattern.compile("(?<![\\p{L}])([A-Z]{2,4})(?![\\p{L}])");
    // WHY only punctuation that ends a token: an apostrophe or hyphen joins the letter to a word ("I'm", "L'FSD",
    // "C'est", "A-rated"), and spelling it there breaks the word.
    private static final Pattern SINGLE_UPPERCASE_LETTER_BEFORE_PUNCTUATION =
            Pattern.compile("(?<![\\p{L}\\p{M}\\p{N}_])([A-Z])(?=[,.;:!?](?:\\s|$))");
    private static final String WORD_START = "(?<![\\p{L}\\p{M}\\p{N}_])";
    private static final String WORD_END = "(?![\\p{L}\\p{M}\\p{N}_])";
    private static final String ROMAN_II_TO_V = "III|IV|II|V";
    // WHY the written-out "mark" takes no I and an upper case numeral only: "mark I" and "mark v" are ordinary text.
    private static final Pattern MARK_NUMBER = Pattern.compile(WORD_START + "(?:"
            + "(?iu:(?:mk|мк)\\.?\\s*(" + ROMAN_II_TO_V + "|I))"
            + "|(?iu:mark|марк)\\s+(" + ROMAN_II_TO_V + "))" + WORD_END);
    // WHY I needs no lower case word after it: "you are Elite I can help" is the pronoun, not the rank.
    private static final Pattern ELITE_RANK_NUMBER = Pattern.compile(
            WORD_START + "((?iu:elite|élite|элита|еліта)\\s+)"
                    + "(" + ROMAN_II_TO_V + "|I(?!\\s+\\p{Ll}))" + WORD_END);
    private static final Pattern ITALIAN_CLASS_LETTER =
            Pattern.compile("(?<=\\b(?iu:classe)\\s)([A-Z])(?![\\p{L}])");
    private static final Map<String, String> ITALIAN_SPOKEN_REPLACEMENTS = Map.ofEntries(
            // WHY split: Supertonic misreads a verb with its pronoun attached. Apart, each keeps its own sound.
            Map.entry("riattivati", "riattiva ti"),
            Map.entry("svegliati", "sveglia ti"),
            Map.entry("ascoltami", "ascolta mi"),
            Map.entry("scegline", "scegli ne"),
            Map.entry("prenderti", "prender ti"),
            Map.entry("fermati", "ferma ti"),
            Map.entry("attivala", "attiva la"),
            // WHY the accent: it marks the stress, which Supertonic otherwise puts on the wrong syllable.
            Map.entry("anomalie", "anomalìe"),
            // WHY respelled: English words, which an Italian voice reads with Italian vowels.
            Map.entry("binding", "bai nding"),
            Map.entry("bindforge", "baindforge"));
    // WHY: a wake word that ends the text gets a full stop, so it does not run into the next utterance. Inside a
    // sentence one would cut it in two, since SupertonicTTS splits on sentence-ending punctuation.
    private static final Set<String> ITALIAN_WAKE_WORDS = Set.of("riattivati", "svegliati", "ascoltami");
    private static final Pattern ITALIAN_SPOKEN_FIXES = Pattern.compile(
            "(?iu)(?<![\\p{L}\\p{M}])("
                    + ITALIAN_SPOKEN_REPLACEMENTS.keySet().stream()
                            .map(Pattern::quote)
                            .collect(Collectors.joining("|"))
                    + ")([,.!?;:]?)(?![\\p{L}\\p{M}])");
    private static final Pattern DECIMAL_WITH_UNIT = Pattern.compile(
            "(?<![\\p{L}\\p{N}])([-−]?\\d+[.,]\\d+)([A-Z])([,.;:!?])?(?![\\p{L}])");
    private static final Pattern COMMA_DECIMAL =
            Pattern.compile("(?<![\\p{L}\\p{N}.])([-−]?\\d+),(\\d+)(?![\\p{L}\\p{N}]|[.,]\\d)");
    // WHY: the model writes thousands either way round whatever the language ("1,234,567 crediti"), and
    // read as a decimal that is a thousand times too small. See groupedNumbersInWords for which are grouping.
    private static final Pattern GROUPED_NUMBER = Pattern.compile(
            "(?<![\\p{L}\\p{N}.,])([-−]?\\d{1,3})((?:([.,])\\d{3})(?:\\3\\d{3})*)(?:(?!\\3)[.,](\\d+))?"
                    + "(?![\\p{L}\\p{N}]|[.,]\\d)");
    // An Elite procedural name's tail, "XO-R b19-3" or "AA-A h0": spelled whole, so no letter is left bare.
    private static final Pattern SECTOR_CODE = Pattern.compile(
            "(?<![\\p{L}\\p{N}])([A-Z]{2})-([A-Z])\\s+([a-h])(\\d+)((?:-\\d+)?)(?![\\p{L}\\p{N}])");
    private static final Pattern LETTER_HYPHENATED_NUMBER =
            Pattern.compile("(?<![\\p{L}\\p{N}])([A-Za-z])(\\d+(?:-\\d+)+)(?![\\p{L}\\p{N}])");
    private static final Map<Language, List<String>> LETTER_NAMES = Map.of(
            Language.EN, List.of("ay", "bee", "see", "dee", "ee", "eff", "gee", "aitch", "eye", "jay", "kay",
                    "ell", "em", "en", "oh", "pee", "cue", "ar", "ess", "tee", "you", "vee", "double you",
                    "ex", "why", "zee"),
            Language.DE, List.of("ah", "beh", "tseh", "deh", "eh", "eff", "geh", "hah", "ee", "jott", "kah",
                    "ell", "emm", "enn", "oh", "peh", "kuh", "err", "ess", "teh", "uh", "fau", "weh", "iks",
                    "ypsilon", "zett"),
            Language.FR, List.of("a", "bé", "cé", "dé", "e", "effe", "gé", "ache", "i", "ji", "ka", "elle",
                    "emme", "enne", "o", "pé", "ku", "erre", "esse", "té", "u", "vé", "double vé", "ixe",
                    "i grec", "zède"),
            Language.ES, List.of("a", "be", "ce", "de", "e", "efe", "ge", "hache", "i", "jota", "ka", "ele",
                    "eme", "ene", "o", "pe", "cu", "erre", "ese", "te", "u", "uve", "doble uve", "equis",
                    "ye", "zeta"),
            Language.IT, List.of("a", "bi", "ci", "di", "e", "effe", "gi", "acca", "i", "i lunga", "cappa",
                    "elle", "emme", "enne", "o", "pi", "cu", "erre", "esse", "ti", "u", "vi", "doppia vu",
                    "ics", "ipsilon", "zeta"),
            Language.PT, List.of("á", "bê", "cê", "dê", "é", "efe", "guê", "agá", "i", "jota", "capa", "ele",
                    "eme", "ene", "ó", "pê", "quê", "erre", "esse", "tê", "u", "vê", "duplo vê", "xis",
                    "i grego", "zê"),
            Language.PTBZ, List.of("á", "bê", "cê", "dê", "é", "efe", "gê", "agá", "i", "jota", "cá", "ele",
                    "eme", "ene", "ó", "pê", "quê", "erre", "esse", "tê", "u", "vê", "dáblio", "xis",
                    "ípsilon", "zê"),
            // Latin letters as a Russian or Ukrainian speaker names them when reading a code aloud.
            Language.RU, List.of("а", "бэ", "цэ", "дэ", "е", "эф", "гэ", "ха", "и", "йот", "ка", "эль",
                    "эм", "эн", "о", "пэ", "ку", "эр", "эс", "тэ", "у", "вэ", "дубль-вэ", "икс", "игрек", "зэт"),
            Language.UK, List.of("а", "бе", "це", "де", "е", "еф", "ге", "ха", "і", "йот", "ка", "ель",
                    "ем", "ен", "о", "пе", "ку", "ер", "ес", "те", "у", "ве", "дубль-ве", "ікс", "ігрек", "зет"));

    private SupertonicTextNormalizer() {
    }

    static String normalize(String text, Language language) {
        // WHY first: left as Roman numerals the acronym pass spells them ("eye eye eye"). As digits, the
        // whole-number pass below says them in the voice's language.
        String withMarkNumbers = markNumbersToSpokenForm(text, language);
        String withEliteRanks = eliteRankNumeralsToDigits(withMarkNumbers);
        String withSectorCodes = sectorCodesInWords(withEliteRanks, language);
        String withSpokenCodes = letterHyphenatedNumbersInWords(withSectorCodes, language);
        String withGroupedNumbers = groupedNumbersInWords(withSpokenCodes, language);
        String withDecimalUnits = decimalUnitsInWords(withGroupedNumbers, language);
        String spoken = SignedAndDecimalNumbers.inWordsIncludingWholeNumbers(
                commaDecimalsInWords(withDecimalUnits, language),
                language);
        String withAcronyms = acronymsInWords(spoken, language);
        String withItalianFixes = language == Language.IT
                ? italianSpokenFixes(italianClassLettersInWords(withAcronyms))
                : withAcronyms;
        return singleUppercaseLettersBeforePunctuation(withItalianFixes, language);
    }

    private static String acronymsInWords(String text, Language language) {
        Matcher matcher = UPPERCASE_ACRONYM.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(spokenLetters(matcher.group(1), language)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String italianClassLettersInWords(String text) {
        Matcher matcher = ITALIAN_CLASS_LETTER.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    letterName(matcher.group(1).charAt(0), Language.IT)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String italianSpokenFixes(String text) {
        Matcher matcher = ITALIAN_SPOKEN_FIXES.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String written = matcher.group(1);
            String word = written.toLowerCase(Locale.ROOT);
            String spelling = ITALIAN_SPOKEN_REPLACEMENTS.get(word);
            if (Character.isUpperCase(written.charAt(0))) {
                spelling = Character.toUpperCase(spelling.charAt(0)) + spelling.substring(1);
            }
            String punctuation = matcher.group(2);
            boolean endsText = text.substring(matcher.end()).isBlank();
            if (punctuation.isEmpty() && endsText && ITALIAN_WAKE_WORDS.contains(word)) {
                punctuation = ".";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(spelling + punctuation));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String singleUppercaseLettersBeforePunctuation(String text, Language language) {
        Matcher matcher = SINGLE_UPPERCASE_LETTER_BEFORE_PUNCTUATION.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    letterName(matcher.group(1).charAt(0), language)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String markNumbersToSpokenForm(String text, Language language) {
        Matcher matcher = MARK_NUMBER.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String numeral = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    markWord(language) + " " + romanNumeralToDigit(numeral)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String eliteRankNumeralsToDigits(String text) {
        Matcher matcher = ELITE_RANK_NUMBER.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + romanNumeralToDigit(matcher.group(2))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String romanNumeralToDigit(String roman) {
        return switch (roman.toUpperCase(Locale.ROOT)) {
            case "I" -> "1";
            case "II" -> "2";
            case "III" -> "3";
            case "IV" -> "4";
            case "V" -> "5";
            default -> throw new IllegalStateException("Unexpected Roman numeral: " + roman);
        };
    }

    private static String markWord(Language language) {
        return switch (language) {
            case EN, FR, ES, IT, PT, PTBZ -> "mark";
            case DE -> "Mark";
            case RU, UK -> "марк";
        };
    }

    private static String commaDecimalsInWords(String text, Language language) {
        if (language == Language.EN) {
            return text;
        }
        Matcher matcher = COMMA_DECIMAL.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String decimal = matcher.group(1).replace(".", "") + "." + matcher.group(2);
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    SignedAndDecimalNumbers.inWordsIncludingWholeNumbers(decimal, language)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Thousands groups, read as one whole number. A separator is grouping when nothing else can explain it:
     * two or more groups ("1.234.567"), a fraction after the other separator ("1,234.5", "1.234,56"), or a
     * comma in English, which never writes a decimal comma. A lone group elsewhere ("135,234", "0.001") is
     * as likely a decimal and is left to the decimal passes.
     */
    private static String groupedNumbersInWords(String text, Language language) {
        Matcher matcher = GROUPED_NUMBER.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String separator = matcher.group(3);
            String groups = matcher.group(2);
            String fraction = matcher.group(4);
            int groupCount = groups.length() / ",234".length();
            boolean grouping = groupCount > 1 || fraction != null
                    || (language == Language.EN && separator.equals(","));
            if (!grouping) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            String whole = matcher.group(1) + groups.replace(separator, "");
            String number = fraction == null ? whole : whole + "." + fraction;
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    SignedAndDecimalNumbers.inWordsIncludingWholeNumbers(number, language)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String sectorCodesInWords(String text, Language language) {
        Matcher matcher = SECTOR_CODE.matcher(text);
        StringBuffer result = new StringBuffer();
        String hyphenName = hyphenName(language);
        while (matcher.find()) {
            StringBuilder spokenCode = new StringBuilder()
                    .append(spokenLetters(matcher.group(1), language))
                    .append(' ').append(hyphenName).append(' ')
                    .append(spokenLetters(matcher.group(2), language))
                    .append(' ').append(letterName(Character.toUpperCase(matcher.group(3).charAt(0)), language))
                    .append(' ').append(SignedAndDecimalNumbers.inWordsIncludingWholeNumbers(
                            matcher.group(4), language));
            if (!matcher.group(5).isEmpty()) {
                spokenCode.append(' ').append(hyphenName).append(' ')
                        .append(SignedAndDecimalNumbers.inWordsIncludingWholeNumbers(
                                matcher.group(5).substring(1), language));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(spokenCode.toString()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String spokenLetters(String letters, Language language) {
        StringBuilder spoken = new StringBuilder();
        for (int i = 0; i < letters.length(); i++) {
            if (i > 0) {
                spoken.append(' ');
            }
            spoken.append(letterName(letters.charAt(i), language));
        }
        return spoken.toString();
    }

    private static String letterHyphenatedNumbersInWords(String text, Language language) {
        Matcher matcher = LETTER_HYPHENATED_NUMBER.matcher(text);
        StringBuffer result = new StringBuffer();
        String hyphenName = hyphenName(language);
        while (matcher.find()) {
            StringBuilder spokenCode = new StringBuilder()
                    .append(letterName(Character.toUpperCase(matcher.group(1).charAt(0)), language));
            String[] numberGroups = matcher.group(2).split("-");
            for (int i = 0; i < numberGroups.length; i++) {
                spokenCode.append(' ');
                if (i > 0) {
                    spokenCode.append(hyphenName).append(' ');
                }
                spokenCode.append(SignedAndDecimalNumbers.inWordsIncludingWholeNumbers(
                        numberGroups[i], language));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(spokenCode.toString()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String hyphenName(Language language) {
        return switch (language) {
            case EN -> "dash";
            case DE -> "Bindestrich";
            case FR -> "tiret";
            case ES -> "guion";
            case IT -> "trattino";
            case PT, PTBZ -> "hífen";
            case RU -> "дефис";
            case UK -> "дефіс";
        };
    }

    private static String decimalUnitsInWords(String text, Language language) {
        Matcher matcher = DECIMAL_WITH_UNIT.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String number = spokenMeasurement(matcher.group(1), language);
            String punctuation = matcher.group(3) == null ? "" : matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    number + " " + letterName(matcher.group(2).charAt(0), language) + punctuation));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String spokenMeasurement(String value, Language language) {
        String normalizedValue = value.replace(',', '.');
        return SignedAndDecimalNumbers.inWords(normalizedValue, language);
    }

    private static String letterName(char letter, Language language) {
        List<String> names = LETTER_NAMES.get(language);
        int index = letter - 'A';
        return names == null || index < 0 || index >= names.size()
                ? String.valueOf(letter)
                : names.get(index);
    }
}
