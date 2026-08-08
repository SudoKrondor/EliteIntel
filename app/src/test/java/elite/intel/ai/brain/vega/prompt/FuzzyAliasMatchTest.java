package elite.intel.ai.brain.vega.prompt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static elite.intel.ai.brain.i18n.AliasVocabulary.tokenize;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The matching rules on their own, with a hand-built vocabulary and no registries, DB or session.
 */
class FuzzyAliasMatchTest {

    /**
     * Stands in for the language's authored words.
     */
    private static final Set<String> VOCABULARY = Set.of(
            "deploy", "landing", "gear", "heat", "sink", "dump", "jump", "hyperspace",
            "request", "docking", "permission", "chat", "what", "panel", "open");

    @ParameterizedTest(name = "a {0}-letter word tolerates {1} edit(s)")
    @CsvSource({"1, 0", "3, 0", "4, 1", "7, 1", "8, 2", "11, 2", "12, 3", "40, 3"})
    void theEditBudgetScalesWithWordLength(int length, int expected) {
        assertEquals(expected, FuzzyAliasMatch.budgetFor(length));
    }

    @Test
    void aMisheardWordIsRepairedWhenEveryOtherWordAgrees() {
        assertTrue(matches("deploy blanding near", "deploy landing gear"));
        assertTrue(matches("request lending permission", "request landing permission"));
    }

    /**
     * The guard the vocabulary exists for: "dump" is a word we authored (deploy_heat_sink says "dump heat"),
     * so it is what the commander said - never one edit away from "jump" and a hyperspace jump.
     */
    @Test
    void aWordWeAuthoredIsNeverRepaired() {
        assertFalse(matches("dump heat", "jump heat"));
        assertFalse(matches("what panel", "chat panel"));
    }

    /**
     * Without an exact word to anchor it, a phrase could drift onto an alias one edit at a time.
     */
    @Test
    void aMultiWordPhraseNeedsOneWordToLandExactly() {
        assertFalse(matches("blanding gearx", "landing gear"));
        assertTrue(matches("blanding gear", "landing gear"));
    }

    @Test
    void aPhraseOfADifferentLengthIsNotAMatch() {
        assertFalse(matches("deploy the blanding near", "deploy landing gear"));
        assertFalse(matches("blanding", "landing gear"));
    }

    /**
     * One word alone has nothing to corroborate it, so it is held to a single edit however long it is.
     */
    @Test
    void aSingleWordPhraseGetsOneEditOnly() {
        assertTrue(matches("hardpointz", "hardpoints"));
        assertFalse(matches("hardpointzz", "hardpoints"));
    }

    @Test
    void anExactPhraseIsNotAFuzzyMatch() {
        assertFalse(matches("deploy landing gear", "deploy landing gear"),
                "the verbatim pass owns that case; this one must not claim it too");
    }

    @Test
    void shortWordsAreHeldToTheExactSpelling() {
        assertFalse(matches("open bay", "open way"));
    }

    @Test
    void theBandedDistanceAgreesWithTheBudget() {
        assertTrue(FuzzyAliasMatch.withinDistance("blanding", "landing", 1));
        assertFalse(FuzzyAliasMatch.withinDistance("blanding", "landing", 0));
        assertTrue(FuzzyAliasMatch.withinDistance("navigation", "mitigation", 3));
        assertFalse(FuzzyAliasMatch.withinDistance("navigation", "mitigation", 2));
        assertFalse(FuzzyAliasMatch.withinDistance("scan", "scanner", 2), "length gap alone exceeds the budget");
    }

    private static boolean matches(String heard, String authored) {
        return FuzzyAliasMatch.phraseMatches(
                tokenize(heard), tokenize(authored), VOCABULARY);
    }

    @Test
    void tokenizationDropsPunctuationAndCase() {
        assertEquals(List.of("request", "docking"), tokenize("Request, docking!"));
    }
}
