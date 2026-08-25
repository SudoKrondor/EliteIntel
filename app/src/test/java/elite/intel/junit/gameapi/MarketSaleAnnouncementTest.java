package elite.intel.junit.gameapi;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.TTSFriendlyNumberConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static elite.intel.util.StringUtls.localizedEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The sale announcement end to end: the template and the number formatting have to agree, because either one
 * alone can put digits back in front of the voice.
 *
 * <p>The figures are the ones from the report - 1320 tonnes of steel for 45,132,120 credits off a carrier.
 * Passed as numbers they reached the voice as "1,320" and "45,132,120", and as "1.320" and "45.132.120" in
 * every locale that groups with dots.
 */
class MarketSaleAnnouncementTest {

    @BeforeEach
    void forceEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @AfterEach
    void restoreEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void aCarrierSizedSaleIsSpokenInWords() {
        assertEquals("Sold one thousand three hundred twenty units of Steel for about forty-five point one million credits.",
                soldUnits(1320, "Steel", 45_132_120L));
    }

    @Test
    void aBatchOfSalesIsSpokenInWords() {
        assertEquals("Sold three commodities for a total of about one hundred nine point one million credits.",
                soldMultiple(3, 109_121_244L));
    }

    /**
     * The locales that group with a dot are the ones that broke: a voice handed "45.132.120" reads it as a
     * decimal. Nothing this announcement says may contain a digit at all.
     */
    @Test
    void noLocaleLeavesADigitInTheSentence() {
        for (Language language : Language.values()) {
            SystemSession.getInstance().setLanguage(language);
            String units = soldUnits(1320, "Steel", 45_132_120L);
            String multiple = soldMultiple(3, 109_121_244L);
            assertFalse(units.matches(".*\\d.*"), language + " units announcement still carries digits: " + units);
            assertFalse(multiple.matches(".*\\d.*"), language + " batch announcement still carries digits: " + multiple);
        }
    }

    private static String soldUnits(int count, String commodity, long totalSale) {
        return localizedEvent("event.market.sold.units",
                TTSFriendlyNumberConverter.formatCountForSpeech(count),
                commodity,
                TTSFriendlyNumberConverter.formatCreditsForSpeech(totalSale));
    }

    private static String soldMultiple(int commodities, long total) {
        return localizedEvent("event.market.sold.multiple",
                TTSFriendlyNumberConverter.formatCountForSpeech(commodities),
                TTSFriendlyNumberConverter.formatCreditsForSpeech(total));
    }
}
