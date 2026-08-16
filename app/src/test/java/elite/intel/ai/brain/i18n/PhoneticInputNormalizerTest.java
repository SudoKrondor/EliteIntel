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
     * The reported utterance. A blanket {@code of -> off} correction used to turn every English preposition
     * into "off", so this arrived as "power OFF the systems" and the model answered that power transfer
     * cannot decrease power.
     */
    @Test
    void heardOfInAPowerRequestMeansToAndNeverOff() {
        LinkedHashMap<String, String> corrections = new EnglishInputNormalizerRules().buildPhoneticMap();

        assertEquals("transfer power to systems",
                PhoneticInputNormalizer.normalize("transfer power of the systems", corrections));
        assertEquals("transfer power to shields",
                PhoneticInputNormalizer.normalize("transfer power of the shields", corrections));
    }

    /**
     * The aliases are authored without the article, so the spoken form has to lose it to stay a reflex.
     */
    @Test
    void theArticleInAPowerRequestCollapsesOntoTheAuthoredAlias() {
        LinkedHashMap<String, String> corrections = new EnglishInputNormalizerRules().buildPhoneticMap();

        assertEquals("power to engines",
                PhoneticInputNormalizer.normalize("power to the engines", corrections));
    }

    /**
     * The preposition itself is ordinary English and must survive: "get us out of dock" is an authored alias,
     * and the old rule rewrote it word for word into something no alias contains.
     */
    @Test
    void theOrdinaryPrepositionIsLeftAlone() {
        LinkedHashMap<String, String> corrections = new EnglishInputNormalizerRules().buildPhoneticMap();

        assertEquals("get us out of dock", PhoneticInputNormalizer.normalize("get us out of dock", corrections));
        assertEquals("what is the distance of sol",
                PhoneticInputNormalizer.normalize("what is the distance of sol", corrections));
    }

    /**
     * What the blanket rule existed for still works, now pinned to the phrases that actually mean "off".
     */
    @Test
    void stillRepairsTheFixedPhrasesWhereOfMeansOff() {
        LinkedHashMap<String, String> corrections = new EnglishInputNormalizerRules().buildPhoneticMap();

        assertEquals("take off", PhoneticInputNormalizer.normalize("take of", corrections));
        assertEquals("turn off night vision", PhoneticInputNormalizer.normalize("turn of night vision", corrections));
        assertEquals("lights off", PhoneticInputNormalizer.normalize("lights of", corrections));
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
