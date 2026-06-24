package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.i18n.de.GermanInputNormalizerRules;
import elite.intel.ai.brain.i18n.en.EnglishInputNormalizerRules;
import elite.intel.ai.brain.i18n.es.SpanishInputNormalizerRules;
import elite.intel.ai.brain.i18n.fr.FrenchInputNormalizerRules;
import elite.intel.ai.brain.i18n.it.ItalianInputNormalizerRules;
import elite.intel.ai.brain.i18n.pt.PortugueseInputNormalizerRules;
import elite.intel.ai.brain.i18n.ru.RussianInputNormalizerRules;
import elite.intel.ai.brain.i18n.uk.UkrainianInputNormalizerRules;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Factory that supplies the correct {@link InputNormalizerProvider} for the current
 * session language and caches the built synonym map per language.
 * <p>
 * Mirrors the structure of {@link AiActionLocalizations} so each language lives in
 * its own file and two localizers can work on different languages simultaneously
 * without conflicts.
 */
public final class InputNormalizerLocalizations {

    private static final EnumMap<Language, CachedRules> CACHE = new EnumMap<>(Language.class);

    private InputNormalizerLocalizations() {
    }

    public static LinkedHashMap<String, String> synonymMap() {
        return rules().synonymMap;
    }

    public static String noiseWordPattern() {
        return rules().noiseWordPattern;
    }

    public static List<String> trashPhrases() {
        return rules().trashPhrases;
    }

    public static Set<String> stopWords() {
        return rules().stopWords;
    }

    private static CachedRules rules() {
        Language lang = SystemSession.getInstance().getLanguage();
        return CACHE.computeIfAbsent(lang, l -> new CachedRules(providerFor(l)));
    }

    private static InputNormalizerProvider providerFor(Language lang) {
        return switch (lang) {
            case EN -> new EnglishInputNormalizerRules();
            case RU -> new RussianInputNormalizerRules();
            case UK -> new UkrainianInputNormalizerRules();
            case DE -> new GermanInputNormalizerRules();
            case FR -> new FrenchInputNormalizerRules();
            case ES -> new SpanishInputNormalizerRules();
            case IT -> new ItalianInputNormalizerRules();
            case PT -> new PortugueseInputNormalizerRules();
        };
    }

    private record CachedRules(LinkedHashMap<String, String> synonymMap, String noiseWordPattern,
                               List<String> trashPhrases, Set<String> stopWords) {
        CachedRules(InputNormalizerProvider provider) {
            this(provider.buildSynonymMap(), provider.noiseWordPattern(), provider.trashPhrases(), provider.stopWords());
        }
    }
}
