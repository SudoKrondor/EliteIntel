package elite.intel.util;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import elite.intel.i18n.Language;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A number written the way it is said aloud, in one of the languages we ship - 1320 as
 * "one thousand three hundred twenty", "eintausenddreihundertzwanzig", "mil trezentos e vinte".
 *
 * <p><b>Why words at all.</b> Every figure the app speaks goes through a TTS engine, and a number handed
 * over as digits is formatted by the reader's locale first: 45132120 reaches the voice as "45,132,120" in
 * English and "45.132.120" in German, Italian and Portuguese, where the group separator is read as a decimal
 * point. Spelling the number out removes the separator from the problem entirely.
 *
 * <p><b>Why ICU and not our own rules.</b> Spelling is where the languages stop agreeing. German inverts and
 * runs together ("fünfundvierzig"), French counts in twenties ("quatre-vingt-dix-sept"), Italian elides
 * ("ventotto"), Spanish contracts the twenties ("veintidós") and the Iberian and Slavic hundreds are
 * irregular words rather than "three" plus "hundred" ("trescientos", "триста"). The composition templates
 * that used to do this could express none of it, so German heard "vierzig fünf" and Russian
 * "три сотен двадцать". The rules now come from CLDR.
 *
 * <p>The mirror of {@link SpokenNumbers}, which reads a number back out of what the commander said.
 */
public final class NumberWords {

    /**
     * ICU marks the seams inside a compound with a soft hyphen ("fünf­und­vierzig") as a hint to
     * typesetters. It is invisible in print and meaningless to a voice, so it comes straight back out.
     */
    private static final char SOFT_HYPHEN = '­';

    /**
     * Enough for the one and two decimal places the credit rounding produces, and no more: the point of the
     * rounding is that nobody hears the last digits of a billion.
     */
    private static final int MAX_FRACTION_DIGITS = 2;

    /**
     * Built once per language - parsing a rule set is not free - and shared. {@link RuleBasedNumberFormat} is
     * not thread safe and the app formats from whatever virtual thread the event landed on, so every call
     * holds the instance's own lock while it formats.
     */
    private static final Map<Language, RuleBasedNumberFormat> SPELLERS = new ConcurrentHashMap<>();

    private NumberWords() {
    }

    /**
     * A whole number in words, e.g. {@code 1320} as "one thousand three hundred twenty".
     */
    public static String of(long value, Language language) {
        RuleBasedNumberFormat speller = spellerFor(language);
        synchronized (speller) {
            return clean(speller.format(value));
        }
    }

    /**
     * A number with a fraction in words, e.g. {@code 45.1} as "forty-five point one". Rounded to two decimal
     * places, and the language decides how the point itself is said ("Komma", "virgule", "целых").
     */
    public static String of(double value, Language language) {
        RuleBasedNumberFormat speller = spellerFor(language);
        synchronized (speller) {
            return clean(speller.format(value));
        }
    }

    private static RuleBasedNumberFormat spellerFor(Language language) {
        return SPELLERS.computeIfAbsent(language, lang -> {
            RuleBasedNumberFormat speller =
                    new RuleBasedNumberFormat(new ULocale(tag(lang)), RuleBasedNumberFormat.SPELLOUT);
            speller.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
            return speller;
        });
    }

    /**
     * The CLDR locale whose spelling rules a language uses. The two Portuguese variants take their own rules
     * rather than sharing: European Portuguese says "dezasseis" where Brazilian says "dezesseis", which is
     * exactly the distinction the separate bundles exist for.
     */
    private static String tag(Language language) {
        return switch (language) {
            case EN -> "en";
            case DE -> "de";
            case FR -> "fr";
            case ES -> "es";
            case IT -> "it";
            case PT -> "pt-PT";
            case PTBZ -> "pt-BR";
            case RU -> "ru";
            case UK -> "uk";
        };
    }

    private static String clean(String spelled) {
        return spelled.indexOf(SOFT_HYPHEN) < 0 ? spelled : spelled.replace(String.valueOf(SOFT_HYPHEN), "");
    }
}
