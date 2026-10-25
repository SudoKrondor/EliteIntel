package elite.intel.ai.mouth.supertonic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;

import elite.intel.i18n.Language;
import elite.intel.util.SignedAndDecimalNumbers;

/**
 * Makes text unambiguous for Supertonic's multilingual tokenizer.
 */
final class SupertonicTextNormalizer {

    // Long uppercase words are emphasis, not acronyms: "RIATTIVA" must stay one word.
    private static final Pattern UPPERCASE_ACRONYM = Pattern.compile("(?<![\\p{L}])([A-Z]{2,4})(?![\\p{L}])");
    private static final Pattern ITALIAN_CLASS_LETTER =
            Pattern.compile("(?i)(?<=\\bclasse\\s)([A-Z])(?![\\p{L}])");
    private static final Pattern DECIMAL_WITH_UNIT = Pattern.compile(
            "(?<![\\p{L}\\p{N}])([-−]?\\d+[.,]\\d+)([A-Z])([,.;:!?])?(?![\\p{L}])");
    private static final Pattern COMMA_DECIMAL =
            Pattern.compile("(?<![\\p{L}\\p{N}.])([-−]?(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)),"
                    + "(\\d+)(?![\\p{L}\\p{N}]|[.,]\\d)");
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
                    "ípsilon", "zê"));

    private SupertonicTextNormalizer() {
    }

    static String normalize(String text, Language language) {
        String withSpokenCodes = letterHyphenatedNumbersInWords(text, language);
        String withDecimalUnits = decimalUnitsInWords(withSpokenCodes, language);
        String spoken = SignedAndDecimalNumbers.inWordsIncludingWholeNumbers(
                commaDecimalsInWords(withDecimalUnits, language),
                language);
        Matcher matcher = UPPERCASE_ACRONYM.matcher(spoken);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String acronym = matcher.group(1);
            StringBuilder spokenAcronym = new StringBuilder();
            for (int i = 0; i < acronym.length(); i++) {
                if (i > 0) {
                    spokenAcronym.append(' ');
                }
                spokenAcronym.append(letterName(acronym.charAt(i), language));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(spokenAcronym.toString()));
        }
        matcher.appendTail(result);
        if (language == Language.IT) {
            Matcher classLetter = ITALIAN_CLASS_LETTER.matcher(result.toString());
            StringBuffer withClassLetters = new StringBuffer();
            while (classLetter.find()) {
                classLetter.appendReplacement(withClassLetters, Matcher.quoteReplacement(
                        letterName(classLetter.group(1).charAt(0), language)));
            }
            classLetter.appendTail(withClassLetters);
            return withClassLetters.toString();
        }
        return result.toString();
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
