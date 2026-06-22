package elite.intel.util;

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
 */
public class TTSFriendlyNumberConverter {

    public static String formatBountyForSpeech(int bounty) {
        if (bounty <= 0) return getText("tts.bounty.none");

        // For small amounts, round to nearest hundred (easier to say)
        if (bounty < 1_000) {
            int rounded = Math.round(bounty / 100f) * 100;
            return getText("tts.bounty.about", smallNumberToWords(rounded));
        }

        // Round to one significant digit for 1k and above
        int rounded = roundToOneSignificant(bounty);

        // Express in words with scale (thousand/million/billion)
        if (rounded >= 1_000_000_000) {
            int billions = rounded / 1_000_000_000;
            return getText("tts.bounty.billions", smallNumberToWords(billions));
        } else if (rounded >= 1_000_000) {
            int millions = rounded / 1_000_000;
            return getText("tts.bounty.millions", smallNumberToWords(millions));
        } else {
            int thousands = rounded / 1_000;
            // thousands can be up to 999 after rounding to one significant digit
            return getText("tts.bounty.thousands", smallNumberToWords(thousands));
        }
    }

    // Rounds positive integer to 1 significant digit (e.g., 322540 -> 300000)
    private static int roundToOneSignificant(int n) {
        if (n <= 0) return 0;
        int magnitude = (int) Math.floor(Math.log10(n));
        int scale = (int) Math.pow(10, magnitude);
        int first = Math.round(n / (float) scale);
        return first * scale;
    }

    // Converts 1..999 that are multiples of 1, 10, or 100 into words (compact)
    private static String smallNumberToWords(int n) {
        if (n == 0) return getText("tts.number.zero");
        // Handle exact hundreds and tens we produce via rounding
        if (n >= 100) {
            int hundreds = n / 100;
            int remainder = n % 100;
            String base = getText("tts.number.hundred", unitsWord(hundreds));
            if (remainder == 0) return base;
            // remainder will be multiple of 10 in our usage
            return getText("tts.number.hundredRemainder", base, belowHundredToWords(remainder));
        }
        return belowHundredToWords(n);
    }

    private static String belowHundredToWords(int n) {
        if (n >= 20) {
            return tensWord(n);
        } else if (n >= 10) {
            return teensWord(n);
        } else {
            return unitsWord(n);
        }
    }

    private static String unitsWord(int n) {
        if (n >= 1 && n <= 9) return getText("tts.number." + n);
        return String.valueOf(n);
    }

    private static String teensWord(int n) {
        if (n >= 10 && n <= 19) return getText("tts.number." + n);
        return String.valueOf(n);
    }

    private static String tensWord(int n) {
        // Assumes n is a multiple of 10 (common after rounding), but handles 20..99
        int tens = n / 10;
        int ones = n % 10;
        if (tens == 1) return teensWord(n); // 10..19
        if (tens < 2 || tens > 9) return String.valueOf(n);
        String tenWord = getText("tts.number." + (tens * 10));
        if (ones == 0) return tenWord;
        return getText("tts.number.tensWithOnes", tenWord, unitsWord(ones));
    }

}