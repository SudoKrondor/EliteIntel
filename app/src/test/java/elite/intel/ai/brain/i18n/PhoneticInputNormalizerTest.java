package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.i18n.en.EnglishInputNormalizerRules;
import elite.intel.ai.brain.i18n.fr.FrenchInputNormalizerRules;
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

    /**
     * The French carrier is a homophone pair, so the STT engine decides which spelling arrives. Both must reach the
     * singular form the French aliases are authored in, and the singular must survive untouched.
     */
    @Test
    void collapsesFrenchCarrierPluralOntoTheAuthoredSingular() {
        LinkedHashMap<String, String> corrections = new FrenchInputNormalizerRules().buildPhoneticMap();

        assertEquals("définis la destination du porte-vaisseau",
                PhoneticInputNormalizer.normalize("définis la destination du porte-vaisseaux", corrections));
        assertEquals("retourne au porte vaisseau",
                PhoneticInputNormalizer.normalize("retourne au porte vaisseaux", corrections));
    }

    /**
     * The singular is already correct, and must not gain a second "x" by being rewritten onto itself.
     */
    @Test
    void leavesTheFrenchCarrierSingularUnchanged() {
        LinkedHashMap<String, String> corrections = new FrenchInputNormalizerRules().buildPhoneticMap();

        assertEquals("trace l'itinéraire vers le porte-vaisseau",
                PhoneticInputNormalizer.normalize("trace l'itinéraire vers le porte-vaisseau", corrections));
    }
}
