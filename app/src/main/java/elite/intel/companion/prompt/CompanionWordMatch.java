package elite.intel.companion.prompt;

import java.util.Locale;

/**
 * Decides whether two single words are "the same word" for the companion's command word-overlap, tolerant of
 * inflected endings (Russian declensions and the like). Two words match when they are equal, share a long
 * enough common start (the same stem with a different ending), or differ by only a few letters (a typo). The
 * letter-difference allowance grows with word length: longer words may differ in more letters.
 * <p>
 * This decides which commands are shown to the model, so it favours catching a real match over strictness - a
 * wrong extra command is cheap (the model still picks the right one), a missed command is invisible to it.
 * Deliberately a companion-local helper (not the DB-backed {@code FuzzySearch}): it compares two plain words,
 * with no database, no candidate list, and a length-scaled budget tuned for short command words.
 */
final class CompanionWordMatch {

    private CompanionWordMatch() {
    }

    /** Below this length a word must match exactly; tolerant rules would over-match short words. */
    private static final int MIN_LEN = 3;
    /** Stem rule needs at least this length and this many shared leading letters. */
    private static final int STEM_MIN_LEN = 5;
    private static final int STEM_MIN_PREFIX = 4;

    /** Whether the two words are the same word up to an inflected ending or a small typo. */
    static boolean similar(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String x = a.toLowerCase(Locale.ROOT).trim();
        String y = b.toLowerCase(Locale.ROOT).trim();
        if (x.isEmpty() || y.isEmpty()) {
            return false;
        }
        if (x.equals(y)) {
            return true;
        }
        int min = Math.min(x.length(), y.length());
        if (min < MIN_LEN) {
            return false; // short words: only exact (already handled above)
        }
        // One word is the whole start of the other: an ending was appended (plurals, verb endings) -
        // ruta/rutas, ship/ships, щит/щиты, contact/contacts. Cheap and language-agnostic.
        String shorter = x.length() <= y.length() ? x : y;
        String longer = x.length() <= y.length() ? y : x;
        if (longer.startsWith(shorter)) {
            return true;
        }
        int common = commonPrefixLength(x, y);
        // Same stem, different ending: the shorter word agrees up to its last couple of letters.
        if (min >= STEM_MIN_LEN && common >= STEM_MIN_PREFIX && common >= min - 2) {
            return true;
        }
        // Typo / minor spelling difference: allow more differing letters the longer the word is.
        int budget = min < 6 ? 0 : (min - 2) / 4;
        return levenshtein(x, y) <= budget;
    }

    private static int commonPrefixLength(String a, String b) {
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    /** Number of single-character insert/delete/replace edits to turn one word into the other. */
    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
