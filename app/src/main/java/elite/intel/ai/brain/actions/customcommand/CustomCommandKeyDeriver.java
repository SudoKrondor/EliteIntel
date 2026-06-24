package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.brain.i18n.AiActionLocalizations;

import java.text.Normalizer;
import java.util.*;

/**
 * Derives a routing-safe {@code actionKey} from a custom command's trigger phrases.
 * <p>
 * The action key is not authored by the user: it is generated from the phrases so that its tokens
 * echo the words the commander actually says. That token overlap is what lets the routing LLM emit
 * the key reliably - a key with no relationship to the phrases (e.g. {@code foo_bar}) routes poorly
 * even when mapped to a perfect phrase.
 * <p>
 * The derivation is script-agnostic on purpose. Latin diacritics fold to ASCII (so French
 * {@code "étoile"} becomes {@code etoile}), but letters of scripts with no Latin decomposition are
 * preserved (so Russian {@code "лететь к миссии"} becomes {@code лететь_к_миссии}). A same-script key
 * keeps maximal overlap with same-script phrases; transliterating to Latin would throw that overlap
 * away. See {@link CustomCommandValidator#SAFE_ID} for the matching format rule.
 */
public final class CustomCommandKeyDeriver {

    private static final String FALLBACK_KEY = "custom_command";

    private CustomCommandKeyDeriver() {
    }

    /**
     * Folds arbitrary text into a single routing-safe token: lowercase letters and decimal digits of
     * any script, separated by underscores. Combining marks (accents) are stripped; everything that is
     * not a lowercase letter or digit becomes an underscore; runs of underscores collapse and the
     * leading/trailing ones are trimmed. The result always matches {@link CustomCommandValidator#SAFE_ID}.
     */
    public static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String folded = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")          // drop accents / combining marks
                .toLowerCase(Locale.ROOT);
        return folded
                .replaceAll("[^\\p{Ll}\\p{Lo}\\p{Nd}]+", "_")  // any non lowercase-letter/digit -> separator
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Derives the base (pre-uniqueness) action key from a comma-separated phrase group.
     * <p>
     * Uses the <em>longest</em> sanitized phrase, regardless of its position in the list. The longest
     * phrasing is usually the most descriptive ("select suite specific tool" ->
     * {@code select_suite_specific_tool}), giving the routing model the richest set of distinguishing
     * tokens to separate this command from similar-sounding ones - and it needs no "list your best
     * phrase first" rule for users to learn. Returns {@code ""} when nothing is usable.
     * <p>
     * Repeated word-tokens are de-duplicated (first occurrence kept) so that a user who ignores the
     * "one phrase per line" guidance and runs every phrasing together on one line still gets a clean
     * key ({@code select suite specific tool select tool suite tool} -> {@code select_suite_specific_tool})
     * rather than one with the same words repeated. The result is then truncated at a word boundary to
     * {@link CustomCommandValidator#MAX_ACTION_KEY_LENGTH}.
     */
    public static String deriveBaseKey(String phrases) {
        List<String> candidates = AiActionLocalizations.splitPhraseGroup(phrases);
        String longest = null;
        for (String phrase : candidates) {
            String token = sanitize(phrase);
            if (token.isEmpty()) {
                continue;
            }
            if (longest == null || token.length() > longest.length()) {
                longest = token;
            }
        }
        return longest == null ? "" : truncate(dedupeTokens(longest), CustomCommandValidator.MAX_ACTION_KEY_LENGTH);
    }

    /**
     * Collapses repeated underscore-separated word-tokens, keeping first-occurrence order.
     */
    private static String dedupeTokens(String token) {
        Set<String> seen = new LinkedHashSet<>();
        for (String part : token.split("_")) {
            if (!part.isEmpty()) {
                seen.add(part);
            }
        }
        return String.join("_", seen);
    }

    /**
     * Derives a unique action key from {@code phrases}, appending a numeric suffix when the base key is
     * already used by another command. Comparison is case-insensitive. {@code takenKeys} should exclude
     * the command's own current key when editing so a command keeps its key across edits.
     */
    public static String deriveUniqueKey(String phrases, Collection<String> takenKeys) {
        String base = deriveBaseKey(phrases);
        if (base.isEmpty()) {
            base = FALLBACK_KEY;
        }
        Set<String> taken = new HashSet<>();
        if (takenKeys != null) {
            for (String key : takenKeys) {
                if (key != null) {
                    taken.add(key.toLowerCase(Locale.ROOT));
                }
            }
        }
        String candidate = base;
        int suffix = 2;
        while (taken.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    /**
     * Truncates a sanitized token to {@code max} characters at the last underscore, then trims trailing underscores.
     */
    private static String truncate(String token, int max) {
        if (token.length() <= max) {
            return token;
        }
        String cut = token.substring(0, max);
        int lastUnderscore = cut.lastIndexOf('_');
        if (lastUnderscore >= CustomCommandValidator.MIN_ACTION_KEY_LENGTH) {
            cut = cut.substring(0, lastUnderscore);
        }
        return cut.replaceAll("_+$", "");
    }
}
