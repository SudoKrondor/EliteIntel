package elite.intel.ai.brain.i18n;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Splitting one alias phrase into what the commander says and what the alias already decided. The distinction
 * under test is literal vs variable argument values: a literal is a real value baked into the phrase, a variable
 * stands in for wording only the commander supplies. Anything malformed counts as variable, so an unparseable
 * alias keeps taking the LLM path instead of executing on a guess.
 */
class AliasPhraseTest {

    @Test
    void literalValueIsAnArgumentTheAliasAlreadySupplies() {
        AliasPhrase phrase = AliasPhrase.parse("ziel fsd {key:fsd}");

        assertEquals("ziel fsd", phrase.spokenText());
        assertEquals(Map.of("key", "fsd"), phrase.literalArguments());
        assertFalse(phrase.hasVariableArgument());
    }

    @Test
    void multiWordLiteralValueSurvivesIntact() {
        AliasPhrase phrase = AliasPhrase.parse("target power distributor {key:power distributor}");

        assertEquals("target power distributor", phrase.spokenText());
        assertEquals(Map.of("key", "power distributor"), phrase.literalArguments());
        assertFalse(phrase.hasVariableArgument());
    }

    @Test
    void placeholderLetterIsAVariable() {
        AliasPhrase phrase = AliasPhrase.parse("erhöhe geschwindigkeit um {key:X}");

        assertEquals("erhöhe geschwindigkeit um", phrase.spokenText());
        assertTrue(phrase.literalArguments().isEmpty());
        assertTrue(phrase.hasVariableArgument());
    }

    @Test
    void choiceValueIsAVariable() {
        AliasPhrase phrase = AliasPhrase.parse("toggle lights {state:true/false}");

        assertTrue(phrase.hasVariableArgument(), "the commander's words decide which of the two it is");
    }

    @Test
    void oneVariableAmongLiteralsStillNeedsTheLlm() {
        AliasPhrase phrase = AliasPhrase.parse("find gold {key:gold, max_distance:Y}");

        assertEquals(Map.of("key", "gold"), phrase.literalArguments());
        assertTrue(phrase.hasVariableArgument(), "max_distance is still unknown");
    }

    @Test
    void multipleBlocksAreAllParsed() {
        AliasPhrase phrase = AliasPhrase.parse("go to {lat:X} and {lon:Y}");

        assertEquals("go to and", phrase.spokenText());
        assertTrue(phrase.hasVariableArgument());
    }

    @Test
    void phraseWithoutArgumentsIsSpokenTextOnly() {
        AliasPhrase phrase = AliasPhrase.parse("nav panel");

        assertEquals("nav panel", phrase.spokenText());
        assertTrue(phrase.literalArguments().isEmpty());
        assertFalse(phrase.hasVariableArgument());
    }

    @Test
    void malformedArgumentCountsAsVariable() {
        assertTrue(AliasPhrase.parse("do it {key}").hasVariableArgument(), "no value at all");
        assertTrue(AliasPhrase.parse("do it {:fsd}").hasVariableArgument(), "no parameter name");
        assertTrue(AliasPhrase.parse("do it {ke y:fsd}").hasVariableArgument(), "invalid parameter name");
    }

    @Test
    void blankOrNullPhraseParsesEmpty() {
        assertEquals("", AliasPhrase.parse("   ").spokenText());
        assertEquals("", AliasPhrase.parse(null).spokenText());
        assertFalse(AliasPhrase.parse(null).hasVariableArgument());
    }
}
