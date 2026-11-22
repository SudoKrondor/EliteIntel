package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.ai.brain.i18n.AliasVocabulary;
import elite.intel.i18n.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Damages every authored alias, one word at a time, and demands that the repair never lands on a
 * <em>different</em> action than the one that owns the phrase.
 *
 * <p>WHY this shape: two actions both matching is safe by construction - {@link ReflexResolver} abandons a tie
 * and the model decides, costing a deterministic route but never firing anything. The failure that matters is
 * a single confident match on the wrong action, because that fires a command the commander did not ask for,
 * with no model and no log line saying a choice was made. So the assertion is not "no collisions" but "no
 * unambiguous wrong winner", which is the property the guards in {@link FuzzyAliasMatch} actually buy.
 *
 * <p>It runs per locale because the vocabulary is per locale: the English alias words alone hold 94 pairs one
 * edit apart across different actions, and no reviewer is going to hold nine languages' worth of that in their
 * head while adding a phrase. A new alias that makes some other action reachable by a one-letter slip fails
 * here rather than in the commander's cockpit.
 */
class AliasRepairSafetyTest {

    /**
     * One action's phrase, pre-tokenized.
     */
    private record Phrase(String actionId, List<String> words) {
    }

    /**
     * The damage substitutes a letter taken from the word itself rather than a fixed one, so a Cyrillic word
     * stays Cyrillic. A cross-script letter would still exercise the alignment, but it would not resemble
     * anything a transcript actually produces, and the locales most in need of this scan are the inflected ones.
     */
    private static char substituteFor(String word, int index) {
        char at = word.charAt(index);
        char first = word.charAt(0);
        return first != at ? first : word.charAt(word.length() - 1);
    }

    @ParameterizedTest(name = "{0}: a one-letter slip never fires another action")
    @EnumSource(Language.class)
    void aDamagedAliasNeverResolvesToADifferentAction(Language language) {
        List<Phrase> phrases = phrasesOf(language);
        Set<String> vocabulary = AliasVocabulary.forLanguage(language);
        List<String> violations = new ArrayList<>();
        int tried = 0;
        int recovered = 0;

        for (Phrase phrase : phrases) {
            for (int i = 0; i < phrase.words().size(); i++) {
                for (List<String> damaged : damage(phrase.words(), i, vocabulary)) {
                    tried++;
                    Set<String> winners = new LinkedHashSet<>();
                    for (Phrase candidate : phrases) {
                        if (FuzzyAliasMatch.phraseMatches(damaged, candidate.words(), vocabulary)) {
                            winners.add(candidate.actionId());
                        }
                    }
                    // A tie is abandoned upstream; only a lone winner fires, so only a lone winner can be wrong.
                    if (winners.size() == 1 && !winners.contains(phrase.actionId())) {
                        violations.add(String.join(" ", damaged)
                                + " (from " + phrase.actionId() + ") -> " + winners.iterator().next());
                    } else if (winners.size() == 1) {
                        recovered++;
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> language + ": a damaged alias resolves to the wrong action:\n  "
                + String.join("\n  ", violations.subList(0, Math.min(20, violations.size()))));
        // Without this the assertion above would also pass on a matcher that never matches anything at all.
        int floor = tried / 2;
        int finalRecovered = recovered;
        int finalTried = tried;
        assertTrue(recovered > floor, () -> String.format(
                "%s: the scan proves nothing unless the repair works - %d of %d damaged aliases recovered",
                language, finalRecovered, finalTried));
    }

    /**
     * The damaged forms of one word: a letter substituted, a letter dropped. Anything that happens to spell a
     * word we authored is discarded - the matcher takes those as heard, so they are not repair candidates.
     */
    private static List<List<String>> damage(List<String> words, int index, Set<String> vocabulary) {
        String word = words.get(index);
        List<List<String>> damaged = new ArrayList<>();
        if (word.length() < 4) {
            return damaged; // below the fuzzy floor: the matcher demands these exactly
        }
        Set<String> variants = new LinkedHashSet<>();
        for (int i = 0; i < word.length(); i++) {
            char substitute = substituteFor(word, i);
            if (substitute != word.charAt(i)) {
                variants.add(word.substring(0, i) + substitute + word.substring(i + 1));
            }
            variants.add(word.substring(0, i) + word.substring(i + 1));
        }
        for (String variant : variants) {
            if (vocabulary.contains(variant)) {
                continue;
            }
            List<String> phrase = new ArrayList<>(words);
            phrase.set(index, variant);
            damaged.add(phrase);
        }
        return damaged;
    }

    private static List<Phrase> phrasesOf(Language language) {
        List<Phrase> phrases = new ArrayList<>();
        for (String key : AiActionAliasTextProvider.keys(language)) {
            String group = AiActionAliasTextProvider.getText(language, key);
            for (String phrase : AiActionLocalizations.splitPhraseGroup(group)) {
                List<String> words = AliasVocabulary.tokenize(AliasPhrase.parse(phrase).spokenText());
                if (!words.isEmpty()) {
                    phrases.add(new Phrase(key, words));
                }
            }
        }
        return phrases;
    }
}
