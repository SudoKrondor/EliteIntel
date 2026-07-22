package elite.intel.junit.util;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.TTSFriendlyNumberConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Spoken-amount formatting resolves all words from the ed_events bundle. Assertions run against the English
 * base bundle unless a test sets another language.
 */
class TTSFriendlyNumberConverterTest {

    @BeforeEach
    void forceEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    /**
     * Several tests set a non-English language on the shared session singleton. Restoring here rather than at
     * the end of each test means a failing assertion cannot leak a foreign locale into another test class.
     */
    @AfterEach
    void restoreEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void zeroOrNegativeIsNoBounty() {
        assertEquals("No bounty", TTSFriendlyNumberConverter.formatBountyForSpeech(0));
        assertEquals("No bounty", TTSFriendlyNumberConverter.formatBountyForSpeech(-50));
    }

    /**
     * A positive bounty carries no rounding of its own any more: it is spoken exactly like every other credit
     * amount, so the app hedges money one way. The specific rounding is covered by the credit tests below;
     * this only pins that the two paths agree, plus one concrete wording so the delegation is anchored.
     */
    @Test
    void positiveBountyUsesTheSharedCreditPolicy() {
        for (int bounty : new int[]{250, 322_540, 4_700_000, 1_900_000_000}) {
            assertEquals(TTSFriendlyNumberConverter.formatCreditsForSpeech(bounty),
                    TTSFriendlyNumberConverter.formatBountyForSpeech(bounty),
                    "bounty " + bounty + " must be spoken by the shared credit policy");
        }
        assertEquals("about four point seven million credits",
                TTSFriendlyNumberConverter.formatBountyForSpeech(4_700_000));
    }

    // --- Spoken credit amounts ---

    @Test
    void amountsUpToTenThousandAreExact() {
        assertEquals("zero credits", TTSFriendlyNumberConverter.formatCreditsForLlm(0));
        assertEquals("four hundred fifty credits", TTSFriendlyNumberConverter.formatCreditsForLlm(450));
        assertEquals("eight thousand four hundred fifty credits",
                TTSFriendlyNumberConverter.formatCreditsForLlm(8_450));
        assertEquals("ten thousand credits", TTSFriendlyNumberConverter.formatCreditsForLlm(10_000));
    }

    @Test
    void amountsUpToAMillionRoundToTheNearestThousand() {
        assertEquals("about ten thousand credits", TTSFriendlyNumberConverter.formatCreditsForLlm(10_001));
        assertEquals("about three hundred forty three thousand credits",
                TTSFriendlyNumberConverter.formatCreditsForLlm(342_600));
    }

    @Test
    void millionsCarryOneDecimal() {
        assertEquals("about twelve point four million credits",
                TTSFriendlyNumberConverter.formatCreditsForLlm(12_449_000));
        // A whole number of millions drops the decimal rather than saying "point zero".
        assertEquals("about five million credits", TTSFriendlyNumberConverter.formatCreditsForLlm(4_980_000));
    }

    @Test
    void billionsCarryTwoDecimals() {
        assertEquals("about one point zero two billion credits",
                TTSFriendlyNumberConverter.formatCreditsForLlm(1_023_309_245L));
        // Trailing zero of the second decimal is dropped: 1.20 -> "one point two".
        assertEquals("about one point two billion credits",
                TTSFriendlyNumberConverter.formatCreditsForLlm(1_200_000_000L));
        assertEquals("about one billion credits",
                TTSFriendlyNumberConverter.formatCreditsForLlm(1_004_000_000L));
    }

    @Test
    void roundingUpPromotesToTheNextScale() {
        assertEquals("about one million credits", TTSFriendlyNumberConverter.formatCreditsForLlm(999_600));
        assertEquals("about one billion credits", TTSFriendlyNumberConverter.formatCreditsForLlm(999_950_000L));
    }

    @Test
    void negativeAmountsAreSpokenAsLosses() {
        assertEquals("minus about four point two million credits",
                TTSFriendlyNumberConverter.formatCreditsForLlm(-4_200_000L));
    }

    @Test
    void nothingIsRenderedAsDigits() {
        // A TTS engine may voice "1.02" as "one dot zero two", or mangle a locale's decimal mark, so
        // no formatted amount may ever contain a digit.
        for (Language language : Language.values()) {
            SystemSession.getInstance().setLanguage(language);
            for (long amount : new long[]{0, 450, 8_450, 342_600, 12_449_000, 1_023_309_245L, -4_200_000L}) {
                String spoken = TTSFriendlyNumberConverter.formatCreditsForSpeech(amount);
                assertFalse(spoken.matches(".*\\d.*"),
                        "digits leaked into " + language + " rendering of " + amount + ": " + spoken);
            }
        }
    }

    @Test
    void creditAmountsFollowActiveLanguage() {
        SystemSession.getInstance().setLanguage(Language.DE);
        assertEquals("etwa eins Komma null zwei Milliarden Credits",
                TTSFriendlyNumberConverter.formatCreditsForSpeech(1_023_309_245L));

        // The LLM-facing rendering stays English regardless of the commander's language; the model
        // translates it as part of the sentence it composes.
        assertEquals("about one point zero two billion credits",
                TTSFriendlyNumberConverter.formatCreditsForLlm(1_023_309_245L));
    }

    /**
     * Exactly one million or one billion inflects the scale noun and numeral in most languages, so it takes a
     * singular template. A plural "{0} million" template would say "one millions" / "eins Millionen" / "un
     * millions". A decimal or any value above one keeps the plural, which those languages want there.
     */
    @Test
    void exactlyOneMillionOrBillionUsesTheSingularForm() {
        SystemSession.getInstance().setLanguage(Language.DE);
        assertEquals("etwa eine Million Credits", TTSFriendlyNumberConverter.formatCreditsForSpeech(1_000_000L));
        assertEquals("etwa eine Milliarde Credits", TTSFriendlyNumberConverter.formatCreditsForSpeech(1_000_000_000L));
        // Above one keeps the plural noun the language uses there.
        assertEquals("etwa zwei Millionen Credits", TTSFriendlyNumberConverter.formatCreditsForSpeech(2_000_000L));

        SystemSession.getInstance().setLanguage(Language.FR);
        assertEquals("environ un million de crédits", TTSFriendlyNumberConverter.formatCreditsForSpeech(1_000_000L));
        assertEquals("environ un milliard de crédits", TTSFriendlyNumberConverter.formatCreditsForSpeech(1_000_000_000L));

        SystemSession.getInstance().setLanguage(Language.EN);
        assertEquals("about one million credits", TTSFriendlyNumberConverter.formatCreditsForLlm(1_000_000L));
        assertEquals("about one billion credits", TTSFriendlyNumberConverter.formatCreditsForLlm(1_000_000_000L));
    }

    /**
     * The singular template must exist in every bundle: a missing key falls back to the English string, which
     * would splice "about one million credits" into an otherwise non-English sentence.
     */
    @Test
    void everyLanguageHasBothSingularAndPluralScaleForms() {
        for (Language language : Language.values()) {
            SystemSession.getInstance().setLanguage(language);
            String singular = TTSFriendlyNumberConverter.formatCreditsForSpeech(1_000_000L);
            String plural = TTSFriendlyNumberConverter.formatCreditsForSpeech(2_000_000L);
            assertFalse(singular.contains("tts.amount"), language + " has no singular million key: " + singular);
            assertFalse(plural.contains("tts.amount"), language + " has no plural million key: " + plural);
        }
    }
}