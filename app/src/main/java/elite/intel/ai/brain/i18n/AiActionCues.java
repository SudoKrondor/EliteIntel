package elite.intel.ai.brain.i18n;

import elite.intel.i18n.Language;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Words an action demands in the commander's own sentence before it may be offered at all.
 *
 * <p>WHY it exists: embedding similarity cannot tell "plot route to the fleet carrier" from "clear the fleet
 * carrier route" - both are mostly "carrier" and "route" - so a destructive action whose aliases share a noun
 * with a harmless one reached the model on sentences that never asked for it, and a weak model picked it. A
 * cue is a word-stem that must literally open one word of the utterance ("cancel" admits "cancelled"), so an
 * action that wipes state is only ever offered when the commander said a wiping verb.
 *
 * <p>Cues live in {@code ai_action_cues*.properties}, keyed by action id. An action with no entry is never gated.
 * The English stems are admitted in every language, because commanders mix English game terms into their own.
 */
public final class AiActionCues {

    private static final String BUNDLE_NAME = "i18n.ai_action_cues";
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);
    private static final Pattern NON_LETTER = Pattern.compile("[^\\p{L}\\p{M}]+");

    private AiActionCues() {
    }

    /**
     * The cue stems gating this action in the given language (English stems included), empty when ungated.
     */
    public static List<String> stems(Language language, String actionId) {
        Set<String> stems = new LinkedHashSet<>(read(Locale.ROOT, actionId));
        stems.addAll(read(AiActionAliasTextProvider.locale(language), actionId));
        return List.copyOf(stems);
    }

    /**
     * Whether the input opens at least one word with one of the stems; an empty stem list admits everything.
     */
    public static boolean admits(List<String> stems, String input) {
        if (stems.isEmpty()) {
            return true;
        }
        if (input == null || input.isBlank()) {
            return false;
        }
        for (String word : NON_LETTER.split(input.toLowerCase(Locale.ROOT))) {
            for (String stem : stems) {
                if (!word.isEmpty() && word.startsWith(stem)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> read(Locale locale, String actionId) {
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, NO_FALLBACK_CONTROL);
        } catch (MissingResourceException e) {
            return List.of();
        }
        if (!bundle.containsKey(actionId)) {
            return List.of();
        }
        List<String> stems = new ArrayList<>();
        for (String stem : bundle.getString(actionId).split(",")) {
            String trimmed = stem.strip().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                stems.add(trimmed);
            }
        }
        return stems;
    }
}
