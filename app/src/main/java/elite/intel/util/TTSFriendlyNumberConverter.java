package elite.intel.util;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import static elite.intel.gameapi.i18n.EventsTextProvider.getText;

/**
 * Utility class for converting numbers into text-to-speech (TTS)-friendly
 * representations for amounts. Primarily designed to assist in generating
 * easily understandable phrases for numerical bounties or values.
 * <p>
 * The phrase around the figure - the hedge, the scale noun, the currency - comes from the
 * {@code ed_events} bundle, so it follows the active UI language. The figure itself is spelled
 * out by {@link NumberWords}, which knows how each language builds a number.
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
        return formatCredits(credits, spokenLanguage());
    }

    /**
     * The language the sentence this amount is embedded in will be spoken in.
     * <p>
     * Not simply the session language: the local Kokoro voice cannot read Cyrillic, so a Russian or
     * Ukrainian commander on it is answered in English, and {@code StringUtls.localizedEvent} resolves the
     * surrounding sentence that way. Reading the session language here instead put Russian numerals inside
     * an English sentence - "Sold один тысяч три сотен двадцать units of Steel". One policy for both halves
     * is what keeps a sentence in one language.
     */
    private static Language spokenLanguage() {
        return AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
    }

    /**
     * Same rounding as {@link #formatCreditsForSpeech(long)}, but always in English, for amounts embedded in
     * event payloads handed to the companion LLM. Those payloads are English by contract; the model
     * translates the finished phrase into the commander's language along with the rest of the sentence.
     */
    public static String formatCreditsForLlm(long credits) {
        return formatCredits(credits, Language.EN);
    }

    /**
     * A plain count - tonnes of cargo, units sold - spelled out in words in the language the sentence around
     * it will be spoken in, with no rounding and no currency word.
     * <p>
     * WHY words: the count reaches the sentence through {@link java.text.MessageFormat}, which groups a
     * number by the reader's locale. A carrier hold of 1320 tonnes came out as "1,320" in English and
     * "1.320" in German and Italian, and the separator is exactly what a TTS engine mishandles - the German
     * voice read a tonnage as a decimal.
     * <p>
     * A count is spoken exactly rather than hedged like a credit amount: "about one thousand tonnes" would
     * be wrong about cargo the commander can count.
     */
    public static String formatCountForSpeech(long count) {
        return NumberWords.of(count, spokenLanguage());
    }

    private static String formatCredits(long credits, Language language) {
        if (credits == 0) return getText(language, "tts.amount.zero");
        if (credits < 0) return getText(language, "tts.amount.negative", formatCredits(-credits, language));

        if (credits <= EXACT_LIMIT) {
            return getText(language, "tts.amount.exact", NumberWords.of(credits, language));
        }

        // Nearest thousand, up to a million. Rounding 999,600 up lands on a million, so let it fall through.
        // The rounded figure is spelled whole - 343,000, not 343 with a "thousand" pasted after it - because
        // German and Italian join the scale onto the number ("dreihundertdreiundvierzigtausend",
        // "trecentoquarantatremila") and Russian inflects it with the count. The template only hedges the
        // figure and names the currency; the language decides how the thousand itself is said.
        long thousands = divideRounded(credits, 1_000L);
        if (thousands < 1_000L) {
            return getText(language, "tts.amount.thousands", NumberWords.of(thousands * 1_000L, language));
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
            return getText(language, "tts.amount.millions", NumberWords.of(tenths / 10.0, language));
        }

        // Nearest ten million, expressed as billions with two decimals. Exactly one billion is singular for
        // the same reason as one million above.
        long hundredths = divideRounded(credits, 10_000_000L);
        if (hundredths == 100) {
            return getText(language, "tts.amount.billion");
        }
        return getText(language, "tts.amount.billions", NumberWords.of(hundredths / 100.0, language));
    }

    /**
     * Rounds {@code value / divisor} to the nearest whole number (halves away from zero).
     */
    private static long divideRounded(long value, long divisor) {
        return (value + divisor / 2) / divisor;
    }
}
