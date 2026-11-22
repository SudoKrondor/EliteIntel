package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.i18n.ResponseTextProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.*;

/**
 * Whether the commander asked for brain trees by name, read from their own words.
 * <p>
 * One tool answers both "where can I find tellurium" and "find brain trees with tellurium", and only the
 * second must never end on a bare planet surface. That qualifier is settled here rather than by a second
 * tool or a flag for the model to fill: two near-identical tools defeat the reflex, and a small local model
 * fills a flag unreliably.
 * <p>
 * Stems are matched as plain substrings, so one entry covers a language's inflections ("мозгов" covers
 * мозговое/мозговых). They name nothing else in Elite's vocabulary, so the match can afford to be generous.
 */
final class BrainTreeMention {

    /** i18n key holding the comma-separated, lower-case brain tree stems for one language. */
    private static final String STEMS_KEY = "rawMaterial.brainTreeStems";

    private BrainTreeMention() {
    }

    /** True when the utterance names brain trees, in the commander's input language or in English. */
    static boolean in(String utterance) {
        return in(utterance, stems(SystemSession.getInstance().getLanguage()));
    }

    /** Test seam: match against explicit stems rather than the live language bundles. */
    static boolean in(String utterance, Collection<String> stems) {
        if (utterance == null || utterance.isBlank()) return false;
        String needle = utterance.toLowerCase(Locale.ROOT);
        return stems.stream().anyMatch(needle::contains);
    }

    /**
     * One language's stems plus the English ones: "brain tree" is the name the game's own English client uses,
     * and several locales' commanders say it untranslated.
     */
    static Set<String> stems(Language commanderLanguage) {
        Set<String> stems = new LinkedHashSet<>(stemsFor(commanderLanguage));
        stems.addAll(stemsFor(Language.EN));
        return stems;
    }

    private static List<String> stemsFor(Language language) {
        return Arrays.stream(ResponseTextProvider.getText(language, STEMS_KEY).split(","))
                .map(stem -> stem.strip().toLowerCase(Locale.ROOT))
                .filter(stem -> !stem.isEmpty())
                .toList();
    }
}
