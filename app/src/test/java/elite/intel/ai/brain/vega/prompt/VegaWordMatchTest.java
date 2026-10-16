package elite.intel.ai.brain.vega.prompt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit test for {@link VegaWordMatch}: word forms from the action training phrases must count as the
 * same word across inflected endings, while genuinely different words must not.
 */
class VegaWordMatchTest {

    /**
     * Same word, different ending - must match (these break exact word-overlap).
     */
    @ParameterizedTest(name = "{0} ~ {1}")
    @CsvSource({
            "ruta,rutas",
            "navegación,navegaciones",
            "triebwerk,triebwerke",
            "kontakte,kontakten",
            "frachtraum,frachtraums",
            "anfluggeschwindigkeit,anfluggeschwindigkeiten",
            "bersaglio,bersagli",
            "scudo,scudi",
            "motore,motori",
            "missione,missioni",
            "lancer,lance",
    })
    void matchesInflectedForms(String a, String b) {
        assertTrue(VegaWordMatch.similar(a, b), a + " should match " + b);
        assertTrue(VegaWordMatch.similar(b, a), b + " should match " + a + " (symmetry)");
    }

    /**
     * Different words - must NOT match, even when they share some letters.
     */
    @ParameterizedTest(name = "{0} !~ {1}")
    @CsvSource({
            "stopp,stoff",
            "ziel,zahl",
            "ruta,ruda",
            "torre,terre",
            "motore,mostra",
            "contacts,container",
            "triebwerk,treibstoff",
    })
    void rejectsDifferentWords(String a, String b) {
        assertFalse(VegaWordMatch.similar(a, b), a + " should NOT match " + b);
    }

    @Test
    void shortWordsRequireExactMatch() {
        assertTrue(VegaWordMatch.similar("uno", "uno"));
        assertFalse(VegaWordMatch.similar("uno", "una")); // 3 letters: only exact
    }
}
