package elite.intel.ai.brain.vega.prompt;

import java.util.List;
import java.util.Set;

/**
 * Decides whether a heard phrase is a damaged rendering of one authored alias phrase - the matching used by
 * {@link ReflexResolver} after its verbatim pass finds nothing.
 * <p>
 * Word-for-word against a whole alias, never word-by-word against a vocabulary. Repairing a single word in
 * isolation is not safe: 94 pairs of English alias words are one edit apart while belonging to different
 * actions, so "nearest word wins" would silently swap commands. Aligning the whole phrase makes the
 * surrounding words carry the decision - in "deploy blanding near", {@code blanding} could be nothing but
 * "landing" and {@code near} could be gear/hear/year/wear, yet only "deploy landing gear" leaves every word
 * agreeing with one action. {@link ReflexResolver} then abandons anyway if two actions both match.
 * <p>
 * Three guards keep it strict, and each exists because of a way it could fire wrongly:
 * <ul>
 *   <li><b>A word in the vocabulary is taken as heard.</b> "dump heat" must never drift to "jump", and "what"
 *       must never become "chat" - both are words we authored, so the commander said them.</li>
 *   <li><b>Same word count.</b> The reflex executes without a model; a phrase with a word more or less than
 *       the alias is a phrase we do not actually recognize.</li>
 *   <li><b>At least one word must land exactly</b> (for phrases of two words or more), so a whole utterance
 *       can never drift onto an alias. A one-word phrase has no anchor available, so it is held to a single
 *       edit instead.</li>
 * </ul>
 * The edit budget scales with word length, because a longer word both survives more damage recognizably and
 * has fewer neighbours to be confused with. Short words get nothing: at four letters an edit is a different
 * word ("near"/"gear"), which is exactly why those only pass when the rest of the phrase agrees.
 *
 * @see elite.intel.ai.brain.i18n.AliasVocabulary
 * @see CompanionWordMatch the looser, stem-tolerant sibling used to <em>offer</em> tools, where a wrong extra
 * candidate is cheap; firing without a model is not, so this one has no stem rule at all
 */
public final class FuzzyAliasMatch {

    /**
     * Below this length a word must be heard exactly: one edit in a three-letter word is a different word.
     */
    private static final int MIN_FUZZY_LENGTH = 4;
    /**
     * Ceiling on the length-scaled budget, so a very long word cannot drift arbitrarily far.
     */
    private static final int MAX_BUDGET = 3;
    /**
     * A one-word phrase has no other word to corroborate it, so it gets the smallest useful budget.
     */
    private static final int SINGLE_WORD_BUDGET = 1;
    /**
     * Letters per allowed edit.
     */
    private static final int LETTERS_PER_EDIT = 4;

    private FuzzyAliasMatch() {
    }

    /**
     * Edits allowed for a word of this length: none below four letters, then one per four letters, capped.
     * 4-7 letters tolerate one, 8-11 two, 12 and above three.
     */
    public static int budgetFor(int length) {
        return length < MIN_FUZZY_LENGTH ? 0 : Math.min(MAX_BUDGET, length / LETTERS_PER_EDIT);
    }

    /**
     * Whether the heard words are a damaged rendering of these authored words, given the language's command
     * vocabulary.
     *
     * @param heard      words as transcribed, lower-cased
     * @param authored   words of one alias phrase, lower-cased
     * @param vocabulary every word appearing in any alias of the language ({@code AliasVocabulary})
     */
    public static boolean phraseMatches(List<String> heard, List<String> authored, Set<String> vocabulary) {
        if (heard.isEmpty() || heard.size() != authored.size()) {
            return false;
        }
        int cap = heard.size() == 1 ? SINGLE_WORD_BUDGET : MAX_BUDGET;
        boolean anyExact = false;
        boolean anyRepaired = false;
        for (int i = 0; i < heard.size(); i++) {
            String spoken = heard.get(i);
            String alias = authored.get(i);
            if (spoken.equals(alias)) {
                anyExact = true;
                continue;
            }
            if (vocabulary.contains(spoken)) {
                return false; // a word we authored somewhere: the commander said it, it is not damage
            }
            int budget = Math.min(cap, budgetFor(Math.min(spoken.length(), alias.length())));
            if (budget == 0 || !withinDistance(spoken, alias, budget)) {
                return false;
            }
            anyRepaired = true;
        }
        return anyRepaired && (anyExact || heard.size() == 1);
    }

    /**
     * Whether two words are within {@code budget} single-character edits.
     * <p>
     * Two rows rather than the full matrix ({@code db.FuzzySearch} keeps that, on longer and rarer input), and
     * banded to {@code |i-j| <= budget} with a per-row early exit: the answer is a yes/no against a small
     * budget, so the cells that could only produce a larger distance are never worth filling in.
     */
    static boolean withinDistance(String a, String b, int budget) {
        if (Math.abs(a.length() - b.length()) > budget) {
            return false;
        }
        int width = b.length();
        int unreachable = budget + 1;
        int[] previous = new int[width + 1];
        int[] current = new int[width + 1];
        for (int j = 0; j <= width; j++) {
            previous[j] = j > budget ? unreachable : j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i > budget ? unreachable : i;
            int rowMin = current[0];
            int from = Math.max(1, i - budget);
            int to = Math.min(width, i + budget);
            for (int j = 1; j <= width; j++) {
                if (j < from || j > to) {
                    current[j] = unreachable;
                    continue;
                }
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), substitution);
                rowMin = Math.min(rowMin, current[j]);
            }
            if (rowMin > budget) {
                return false;
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[width] <= budget;
    }
}
