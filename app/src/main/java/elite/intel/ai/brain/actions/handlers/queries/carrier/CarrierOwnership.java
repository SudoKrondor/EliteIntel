package elite.intel.ai.brain.actions.handlers.queries.carrier;

import elite.intel.ai.brain.i18n.ResponseTextProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.*;

/**
 * Which of the two carriers Elite exposes a commander meant: their own fleet carrier, or their squadron's.
 * <p>
 * Both carriers answer identical questions (fuel, finances, route, ETA), so the companion offers ONE tool per
 * question and resolves the owner HERE, from the commander's own words. Keeping a fleet tool beside a near-identical
 * squadron tool made semantic retrieval and the small local model distinguish sibling functions from phrases that
 * differ by one qualifier ("carrier status" / "squadron carrier status").
 * <p>
 * The commander is assumed to mean their own carrier unless they say otherwise: bare "carrier" is how commanders
 * refer to the one they own, and the squadron carrier is the marked, explicitly-named case.
 */
public enum CarrierOwnership {
    FLEET("fleet carrier"),
    SQUADRON("squadron carrier");

    /**
     * i18n key holding the comma-separated, lower-case squadron stems for one language.
     */
    private static final String SQUADRON_STEMS_KEY = "carrier.squadronStems";

    private final String label;

    CarrierOwnership(String label) {
        this.label = label;
    }

    /**
     * How a query's data names this carrier, so the spoken answer says which one it reported. English, like
     * every other field handed to the model, which speaks the answer in the commander's language.
     */
    public String label() {
        return label;
    }

    /**
     * The carrier the utterance names, defaulting to the commander's own.
     */
    public static CarrierOwnership resolve(String utterance) {
        return resolve(utterance, squadronStems());
    }

    /**
     * Test seam: resolve against explicit stems rather than the live language bundles.
     * <p>
     * Stems are matched as plain substrings, not whole words, so one entry covers a language's whole inflectional
     * family ("эскадрил" covers эскадрилья/эскадрильи/эскадрильей). That makes the match deliberately generous:
     * these stems occur in no other Elite vocabulary, so a false positive costs nothing a false negative doesn't
     * cost more.
     */
    static CarrierOwnership resolve(String utterance, Collection<String> squadronStems) {
        if (utterance == null || utterance.isBlank()) {
            return FLEET;
        }
        String needle = utterance.toLowerCase(Locale.ROOT);
        for (String stem : squadronStems) {
            if (needle.contains(stem)) {
                return SQUADRON;
            }
        }
        return FLEET;
    }

    /**
     * The commander's INPUT language (as the aliases use), not the AI's response language: these stems are
     * matched against what the commander said, not against what the companion is about to say back.
     */
    private static Set<String> squadronStems() {
        return squadronStems(SystemSession.getInstance().getLanguage());
    }

    /**
     * One language's stems plus the English ones.
     * <p>
     * WHY the English union: "squadron" is a loanword several locales' own aliases already use verbatim (the
     * German and Italian alias groups are written around it), and the diagnostics harness feeds English
     * utterances regardless of the configured language. Adding English can only recognise a squadron the
     * commander did name; it can never turn a fleet utterance into a squadron one, because no language's word
     * for the commander's own carrier contains an English squadron stem.
     */
    static Set<String> squadronStems(Language commanderLanguage) {
        Set<String> stems = new LinkedHashSet<>(stemsFor(commanderLanguage));
        stems.addAll(stemsFor(Language.EN));
        return stems;
    }

    private static List<String> stemsFor(Language language) {
        return Arrays.stream(ResponseTextProvider.getText(language, SQUADRON_STEMS_KEY).split(","))
                .map(stem -> stem.strip().toLowerCase(Locale.ROOT))
                .filter(stem -> !stem.isEmpty())
                .toList();
    }
}
