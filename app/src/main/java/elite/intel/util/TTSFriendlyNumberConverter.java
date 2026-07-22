package elite.intel.util;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import static elite.intel.gameapi.i18n.EventsTextProvider.getText;

/**
 * Utility class for converting numbers into text-to-speech (TTS)-friendly
 * representations for amounts. Primarily designed to assist in generating
 * easily understandable phrases for numerical bounties or values.
 * <p>
 * All spoken words and phrase templates are resolved from the {@code ed_events}
 * bundle at call time, so the output follows the active UI language. Composition
 * patterns (e.g. {@code tts.number.hundred}) use {@link java.text.MessageFormat}
 * placeholders so translators can reorder the parts per language.
 * <p>
 * Everything is spelled out in words - never digits. A TTS engine reading
 * {@code "1.02"} may voice it as "one dot zero two" or mangle the separator
 * entirely once the locale's decimal mark changes, so the digits never reach it.
 */
public class TTSFriendlyNumberConverter {

    /**
     * Below this, a credit amount is spoken exactly; above it, it is rounded and hedged with "about".
     */
    private static final long EXACT_LIMIT = 10_000L;

    /**
     * A scanned ship's bounty, spoken in the active UI language. Zero means the ship is clean, which reads as
     * "No bounty" rather than "zero credits"; any positive bounty is spoken with the same rounding as every
     * other credit amount (see {@link #formatCreditsForSpeech(long)}), so the whole app hedges money one way.
     */
    public static String formatBountyForSpeech(int bounty) {
        if (bounty <= 0) return getText("tts.bounty.none");
        return formatCreditsForSpeech(bounty);
    }

    /**
     * Renders a credit amount the way a person would say it out loud, in the active UI language.
     * Precision drops as the amount grows, because nobody hears the last six digits of a billion:
     * <ul>
     *   <li>up to 10,000 - exact ("eight thousand four hundred fifty credits")</li>
     *   <li>up to 1,000,000 - nearest thousand ("about three hundred forty three thousand credits")</li>
     *   <li>up to 1,000,000,000 - nearest hundred thousand ("about twelve point four million credits")</li>
     *   <li>above that - nearest ten million ("about one point zero two billion credits")</li>
     * </ul>
     * Use this for text spoken straight to the commander. Text handed to the LLM to narrate should use
     * {@link #formatCreditsForLlm(long)} instead, so the model receives it in the language it composes from.
     */
    public static String formatCreditsForSpeech(long credits) {
        return formatCredits(credits, SystemSession.getInstance().getLanguage());
    }

    /**
     * Same rounding as {@link #formatCreditsForSpeech(long)}, but always in English, for amounts embedded in
     * event payloads handed to the companion LLM. Those payloads are English by contract; the model
     * translates the finished phrase into the commander's language along with the rest of the sentence.
     */
    public static String formatCreditsForLlm(long credits) {
        return formatCredits(credits, Language.EN);
    }

    private static String formatCredits(long credits, Language language) {
        if (credits == 0) return getText(language, "tts.amount.zero");
        if (credits < 0) return getText(language, "tts.amount.negative", formatCredits(-credits, language));

        if (credits <= EXACT_LIMIT) {
            return getText(language, "tts.amount.exact", numberToWords(credits, language));
        }

        // Nearest thousand, up to a million. Rounding 999,600 up lands on a million, so let it fall through.
        long thousands = divideRounded(credits, 1_000L);
        if (thousands < 1_000L) {
            return getText(language, "tts.amount.thousands", numberToWords(thousands, language));
        }

        // Nearest hundred thousand, expressed as millions with one decimal. Exactly one million takes a
        // singular template: in most languages the scale noun and numeral inflect for it ("one million", "eine
        // Million", "un million"), so a plural "{0} million" template would say "one millions". A decimal or
        // any value above one keeps the plural form, which is what those languages want there.
        long tenths = divideRounded(credits, 100_000L);
        if (tenths == 10) {
            return getText(language, "tts.amount.million");
        }
        if (tenths < 10_000L) {
            return getText(language, "tts.amount.millions", scaledToWords(tenths, 10, language));
        }

        // Nearest ten million, expressed as billions with two decimals. Exactly one billion is singular for
        // the same reason as one million above.
        long hundredths = divideRounded(credits, 10_000_000L);
        if (hundredths == 100) {
            return getText(language, "tts.amount.billion");
        }
        return getText(language, "tts.amount.billions", scaledToWords(hundredths, 100, language));
    }

    /**
     * Rounds {@code value / divisor} to the nearest whole number (halves away from zero).
     */
    private static long divideRounded(long value, long divisor) {
        return (value + divisor / 2) / divisor;
    }

    /**
     * Spells a fixed-point value out with its fractional digits, e.g. {@code 124 / 10} as
     * "twelve point four" and {@code 102 / 100} as "one point zero two". A fraction of zero,
     * or one whose digits are all trailing zeros, is dropped entirely - "twelve", not "twelve point zero".
     */
    private static String scaledToWords(long scaled, int scale, Language language) {
        long whole = scaled / scale;
        long fraction = scaled % scale;
        String wholeWords = numberToWords(whole, language);
        if (fraction == 0) return wholeWords;

        // Fixed width so 102/100 keeps its leading zero ("zero two"), then trailing zeros go ("120" -> "1").
        int width = String.valueOf(scale).length() - 1;
        String digits = String.format("%0" + width + "d", fraction).replaceAll("0+$", "");

        StringBuilder fractionWords = new StringBuilder();
        for (char digit : digits.toCharArray()) {
            if (!fractionWords.isEmpty()) fractionWords.append(' ');
            fractionWords.append(digitWord(digit - '0', language));
        }
        return getText(language, "tts.number.point", wholeWords, fractionWords.toString());
    }

    private static String digitWord(int digit, Language language) {
        return digit == 0 ? getText(language, "tts.number.zero") : unitsWord(digit, language);
    }

    /**
     * Spells out 0..999,999 in words. Above that the caller has already reduced to a scale word.
     */
    private static String numberToWords(long n, Language language) {
        if (n == 0) return getText(language, "tts.number.zero");
        if (n < 1_000) return smallNumberToWords((int) n, language);

        long thousands = n / 1_000;
        long remainder = n % 1_000;
        String base = getText(language, "tts.number.thousand", smallNumberToWords((int) thousands, language));
        if (remainder == 0) return base;
        return getText(language, "tts.number.thousandRemainder", base, smallNumberToWords((int) remainder, language));
    }

    // Converts 1..999 that are multiples of 1, 10, or 100 into words (compact)
    private static String smallNumberToWords(int n, Language language) {
        if (n == 0) return getText(language, "tts.number.zero");
        // Handle exact hundreds and tens we produce via rounding
        if (n >= 100) {
            int hundreds = n / 100;
            int remainder = n % 100;
            String base = getText(language, "tts.number.hundred", unitsWord(hundreds, language));
            if (remainder == 0) return base;
            // remainder will be multiple of 10 in our usage
            return getText(language, "tts.number.hundredRemainder", base, belowHundredToWords(remainder, language));
        }
        return belowHundredToWords(n, language);
    }

    private static String belowHundredToWords(int n, Language language) {
        if (n >= 20) {
            return tensWord(n, language);
        } else if (n >= 10) {
            return teensWord(n, language);
        } else {
            return unitsWord(n, language);
        }
    }

    private static String unitsWord(int n, Language language) {
        if (n >= 1 && n <= 9) return getText(language, "tts.number." + n);
        return String.valueOf(n);
    }

    private static String teensWord(int n, Language language) {
        if (n >= 10 && n <= 19) return getText(language, "tts.number." + n);
        return String.valueOf(n);
    }

    private static String tensWord(int n, Language language) {
        // Assumes n is a multiple of 10 (common after rounding), but handles 20..99
        int tens = n / 10;
        int ones = n % 10;
        if (tens == 1) return teensWord(n, language); // 10..19
        if (tens < 2 || tens > 9) return String.valueOf(n);
        String tenWord = getText(language, "tts.number." + (tens * 10));
        if (ones == 0) return tenWord;
        return getText(language, "tts.number.tensWithOnes", tenWord, unitsWord(ones, language));
    }

}
