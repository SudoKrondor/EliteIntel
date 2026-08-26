package elite.intel.ai.brain.i18n;

import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every acoustic correction must still be reachable, in every locale.
 *
 * <p>{@link PhoneticInputNormalizer} walks the map in insertion order and rewrites the whole transcript at each
 * step, so an earlier correction that rewrites part of a later correction's key makes that later entry dead:
 * the text it was written to repair no longer exists by the time its turn comes. Nothing reports this - the
 * mishear simply reaches the model uncorrected, and the rule sits in the file looking like it works.
 *
 * <p>Found this way and fixed in EN: {@code "did" -> "deploy"} ran before {@code "did ploy" -> "deploy"}, and
 * {@code "spectrum scan" -> "scan system"} ran before both {@code "full spectrum scan" -> "FSS"} and
 * {@code "full-spectrum scan" -> "FSS"}. The fix is ordering, not deletion: the longer phrase goes first.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhoneticMapOrderingTest {

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @ParameterizedTest(name = "{0} leaves every correction reachable")
    @EnumSource(Language.class)
    void noCorrectionIsShadowedByAnEarlierOne(Language language) {
        SystemSession.getInstance().setLanguage(language);
        try {
            LinkedHashMap<String, String> applied = new LinkedHashMap<>();
            List<String> shadowed = new ArrayList<>();
            for (Map.Entry<String, String> correction : InputNormalizerLocalizations.phoneticMap().entrySet()) {
                String heard = correction.getKey();
                String afterEarlierRules = PhoneticInputNormalizer.normalize(heard, applied);
                if (!afterEarlierRules.equalsIgnoreCase(heard)) {
                    shadowed.add("\"" + heard + "\" -> \"" + correction.getValue()
                            + "\" is unreachable: an earlier rule already turns it into \"" + afterEarlierRules + "\"");
                }
                applied.put(heard, correction.getValue());
            }

            assertTrue(shadowed.isEmpty(),
                    () -> language + " has acoustic corrections that can never fire:\n  "
                            + String.join("\n  ", shadowed));
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    @ParameterizedTest(name = "{0} has no correction that corrects nothing")
    @EnumSource(Language.class)
    void noCorrectionMapsAWordToItself(Language language) {
        SystemSession.getInstance().setLanguage(language);
        try {
            List<String> noOps = InputNormalizerLocalizations.phoneticMap().entrySet().stream()
                    .filter(correction -> correction.getKey().trim().equalsIgnoreCase(correction.getValue().trim()))
                    .map(correction -> "\"" + correction.getKey() + "\"")
                    .toList();

            assertTrue(noOps.isEmpty(),
                    () -> language + " maps these words to themselves, which does nothing: "
                            + String.join(", ", noOps));
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }
}
