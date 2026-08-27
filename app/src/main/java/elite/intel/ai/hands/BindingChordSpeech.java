package elite.intel.ai.hands;

import elite.intel.util.StringUtls;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders Elite Dangerous key chords as words a speech voice can read, for the spoken binding warnings.
 * <p>
 * WHY this is not {@code BindingSlotDisplayFormatter}: that one formats a chord for the Bindings tab,
 * where the reader has the key in front of them and abbreviation is the point - it renders
 * {@code Key_LeftControl} as "Left Ctrl", and falls back to the bare token, so {@code Key_UpArrow}
 * comes out "UpArrow". Neither survives being read aloud; this renders the same chord as separated
 * whole words instead.
 * <p>
 * The key names stay in English in every language, matching how the Bindings tab labels them: they
 * name the legend printed on the physical keyboard, not a translatable game term.
 * <p>
 * Pure and side-effect free.
 */
public final class BindingChordSpeech {

    private static final String KEY_PREFIX = "Key_";

    /**
     * Modifiers first, then alphabetically - a chord is an unordered key-set (Elite's
     * Primary/Modifier slots are positional only), so it needs an imposed order to read the same way twice.
     */
    private static final Comparator<String> MODIFIERS_FIRST =
            Comparator.comparing((String token) -> !isModifier(token))
                    .thenComparing(Comparator.naturalOrder());

    /**
     * The distinct chords of several conflicts, spoken - {@code ["A", "D", "S", "W"]}.
     * <p>
     * Deduplicated because one chord usually appears in more than one conflict. Returned as a list
     * rather than a joined sentence because the warning needs the count as well as the keys: its
     * localized pattern picks singular or plural wording from it.
     * <p>
     * Sorted alphabetically, and deliberately not in keyboard-layout order: a chord is a {@link Set},
     * so some order has to be imposed for the same bindings to produce the same sentence twice, and
     * alphabetical is the one order that stays meaningful for keys that form no familiar shape.
     */
    public static List<String> distinctChords(Collection<Set<String>> chords) {
        return chords.stream()
                .map(BindingChordSpeech::describe)
                .filter(spoken -> !spoken.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * One chord as spoken words - "W", or "Left Control plus W" when it carries modifiers.
     * <p>
     * An absent chord yields an empty string rather than throwing. This is graceful degradation on
     * purpose, not a swallowed failure - see the reasoning on the guard itself.
     */
    public static String describe(Set<String> chord) {
        // WHY: deliberately lenient, against this project's usual fail-fast rule. A Conflict cannot
        // carry an absent chord (the scanner builds it with Set.copyOf of the key-set it just matched
        // on, and drops empty key-sets before pairing at all), so this branch is unreachable from
        // production. It stays because of where the only caller runs: KeyBindCheck.check() sits inside
        // the AppController service-start try block that catches RuntimeException, stops the whole
        // service registry and transitions to STOPPED. Throwing here would take the app down at
        // startup over a WARNING about a misconfiguration, which is a far worse outcome than a
        // clumsily worded sentence. distinctChords drops whatever this returns empty, so a stray
        // empty chord cannot reach the spoken list or skew its count either.
        if (chord == null || chord.isEmpty()) {
            return "";
        }
        return chord.stream()
                .sorted(MODIFIERS_FIRST)
                .map(BindingChordSpeech::keyName)
                .collect(Collectors.joining(" plus "));
    }

    /**
     * An Elite key token as separated words - {@code Key_UpArrow} to "Up Arrow", {@code Key_Numpad_4}
     * to "Numpad 4". A token from a device other than the keyboard is humanized as it stands rather
     * than dropped, so an unexpected one is still named instead of vanishing from the warning.
     */
    private static String keyName(String token) {
        String bare = token.startsWith(KEY_PREFIX) ? token.substring(KEY_PREFIX.length()) : token;
        // WHY: humanizeBindingName is written for ACTION names (CamTranslateLeft -> "Cam Translate
        // Left"), and is borrowed here because splitting camel case and underscores is the same job
        // for a key token. Two consequences to keep in mind before editing either side: it carries
        // action-only rules that happen to be inert on key tokens (the "HUD" spacing rule), and its
        // digit rule fires before its underscore rule, so "Numpad_4" comes back as "Numpad  4" -
        // the space collapse below is load-bearing, not defensive.
        return StringUtls.humanizeBindingName(bare).replaceAll("\\s{2,}", " ").trim();
    }

    private static boolean isModifier(String token) {
        return token.endsWith("Shift") || token.endsWith("Control") || token.endsWith("Alt");
    }

    private BindingChordSpeech() {
    }
}
