package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.i18n.en.EnglishInputNormalizerRules;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies that companion routing receives only safe, whole-phrase acoustic corrections. */
class PhoneticInputNormalizerTest {

    @Test
    void correctsKnownEnglishCarrierConfusion() {
        String normalized = PhoneticInputNormalizer.normalize(
                "open fleet career management panel", new EnglishInputNormalizerRules().buildPhoneticMap());

        assertEquals("open fleet carrier management panel", normalized);
    }

    @Test
    void doesNotApplyBroaderSynonymRules() {
        String normalized = PhoneticInputNormalizer.normalize(
                "combat mode", new EnglishInputNormalizerRules().buildPhoneticMap());

        assertEquals("combat mode", normalized);
    }

    @Test
    void replacementsRespectWholeWordBoundaries() {
        LinkedHashMap<String, String> corrections = new LinkedHashMap<>();
        corrections.put("of", "off");

        assertEquals("profile off cargo", PhoneticInputNormalizer.normalize("profile of cargo", corrections));
    }
}
