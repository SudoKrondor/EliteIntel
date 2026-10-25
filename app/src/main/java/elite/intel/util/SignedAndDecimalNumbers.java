package elite.intel.util;

import elite.intel.i18n.Language;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spells out the numbers a local voice cannot read from digits: a negative one and one with a fraction.
 * <p>
 * Supertonic drops the minus sign ("-233" is heard as "two hundred thirty-three"), and the sherpa pipeline
 * flattens every dot before synthesis, so "62.123" reached both local engines as "62 123". In words, the sign
 * and the point are part of the number and nothing downstream can lose them. The language decides how they
 * are said ("minus", "moins", "meno").
 * <p>
 * Plain runs of digits are never touched: the engines read them correctly, and Elite's system names are full
 * of them ("Col 285 Sector XY-Z c12-34"). A number glued to a letter, or one piece of a longer dotted run
 * such as a version string, is left alone for the same reason.
 */
public final class SignedAndDecimalNumbers {

    private static final Pattern SIGNED_OR_DECIMAL = Pattern.compile(
            "(?<![\\p{L}\\p{N}]|\\d[.,])([-−])?(\\d+)(?:\\.(\\d+))?(?![\\p{L}\\p{N}]|[.,]\\d)");

    private SignedAndDecimalNumbers() {
    }

    public static String inWords(String text, Language language) {
        return inWords(text, language, false);
    }

    /**
     * Spells standalone whole numbers too. This is needed by voices whose tokenizer handles long digit runs
     * unreliably; the default keeps whole numbers untouched because Elite system names contain meaningful IDs.
     */
    public static String inWordsIncludingWholeNumbers(String text, Language language) {
        return inWords(text, language, true);
    }

    private static String inWords(String text, Language language, boolean includeWholeNumbers) {
        if (text == null || text.isEmpty()) return text;
        Matcher matcher = SIGNED_OR_DECIMAL.matcher(text);
        StringBuilder spoken = new StringBuilder();
        while (matcher.find()) {
            boolean negative = matcher.group(1) != null;
            String fraction = matcher.group(3);
            if (!includeWholeNumbers && !negative && fraction == null) continue;
            BigDecimal value = new BigDecimal(matcher.group(2) + (fraction == null ? "" : "." + fraction));
            BigDecimal signed = negative ? value.negate() : value;
            String words = NumberWords.ofWritten(signed, language);
            matcher.appendReplacement(spoken, Matcher.quoteReplacement(words));
        }
        matcher.appendTail(spoken);
        return spoken.toString();
    }
}
