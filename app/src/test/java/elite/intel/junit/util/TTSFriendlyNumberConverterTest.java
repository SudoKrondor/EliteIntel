package elite.intel.junit.util;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.TTSFriendlyNumberConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Spoken-bounty formatting now resolves all words from the ed_events bundle.
 * Assertions run against the English base bundle.
 */
class TTSFriendlyNumberConverterTest {

    @BeforeEach
    void forceEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void zeroOrNegativeIsNoBounty() {
        assertEquals("No bounty", TTSFriendlyNumberConverter.formatBountyForSpeech(0));
        assertEquals("No bounty", TTSFriendlyNumberConverter.formatBountyForSpeech(-50));
    }

    @Test
    void smallAmountRoundsToNearestHundred() {
        // 250 -> 300
        assertEquals("about three hundred credits", TTSFriendlyNumberConverter.formatBountyForSpeech(250));
    }

    @Test
    void thousandsRoundToOneSignificantDigit() {
        // 322540 -> 300000 -> 300 thousand
        assertEquals("roughly three hundred thousand credits",
                TTSFriendlyNumberConverter.formatBountyForSpeech(322_540));
    }

    @Test
    void millionsRoundToOneSignificantDigit() {
        // 4_700_000 -> 5_000_000
        assertEquals("roughly five million credits",
                TTSFriendlyNumberConverter.formatBountyForSpeech(4_700_000));
    }

    @Test
    void billionsRoundToOneSignificantDigit() {
        // 1_900_000_000 -> 2_000_000_000
        assertEquals("roughly two billion credits",
                TTSFriendlyNumberConverter.formatBountyForSpeech(1_900_000_000));
    }

    @Test
    void outputFollowsActiveLanguage() {
        // 4_700_000 -> 5_000_000 -> single-word "five million" in each language's bundle.
        SystemSession.getInstance().setLanguage(Language.DE);
        assertEquals("ungefähr fünf Millionen Credits",
                TTSFriendlyNumberConverter.formatBountyForSpeech(4_700_000));
        assertEquals("Kein Kopfgeld", TTSFriendlyNumberConverter.formatBountyForSpeech(0));

        SystemSession.getInstance().setLanguage(Language.RU);
        assertEquals("примерно пять миллионов кредитов",
                TTSFriendlyNumberConverter.formatBountyForSpeech(4_700_000));
        assertEquals("Награды нет", TTSFriendlyNumberConverter.formatBountyForSpeech(0));

        SystemSession.getInstance().setLanguage(Language.EN);
    }
}