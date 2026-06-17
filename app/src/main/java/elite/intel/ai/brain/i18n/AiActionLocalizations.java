package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.i18n.de.GermanAiActionAliases;
import elite.intel.ai.brain.i18n.en.EnglishAiActionAliases;
import elite.intel.ai.brain.i18n.es.SpanishAiActionAliases;
import elite.intel.ai.brain.i18n.fr.FrenchAiActionAliases;
import elite.intel.ai.brain.i18n.pt.PortugueseAiActionAliases;
import elite.intel.ai.brain.i18n.ru.RussianAiActionAliases;
import elite.intel.ai.brain.i18n.uk.UkrainianAiActionAliases;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;

import java.util.*;

public final class AiActionLocalizations {

    private AiActionLocalizations() {
    }

    private static AiActionAliasProvider provider() {
        return switch (SystemSession.getInstance().getLanguage()) {
            case EN -> new EnglishAiActionAliases();
            case RU -> new RussianAiActionAliases();
            case UK -> new UkrainianAiActionAliases();
            case DE -> new GermanAiActionAliases();
            case FR -> new FrenchAiActionAliases();
            case ES -> new SpanishAiActionAliases();
            case PT -> new PortugueseAiActionAliases();
        };
    }

    public static void addAliases(Map<String, String> map, Status status, boolean isDryRun) {
        provider().addAliases(map, status, isDryRun);
    }

    public static List<String> phrasesForAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return List.of();
        }

        Map<String, String> aliasesByPhraseGroup = new LinkedHashMap<>();
        provider().addAliases(aliasesByPhraseGroup, Status.getInstance(), true);
        return aliasesByPhraseGroup.entrySet().stream()
                .filter(entry -> actionId.equalsIgnoreCase(entry.getValue()))
                .flatMap(entry -> splitPhraseGroup(entry.getKey()).stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Splits an alias group on top-level commas while preserving commas inside parameter templates.
     */
    public static List<String> splitPhraseGroup(String phraseGroup) {
        if (phraseGroup == null || phraseGroup.isBlank()) {
            return List.of();
        }

        List<String> phrases = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int templateDepth = 0;
        for (int i = 0; i < phraseGroup.length(); i++) {
            char c = phraseGroup.charAt(i);
            if (c == '{') {
                templateDepth++;
            } else if (c == '}' && templateDepth > 0) {
                templateDepth--;
            }

            if (c == ',' && templateDepth == 0) {
                addPhrase(phrases, current);
            } else {
                current.append(c);
            }
        }
        addPhrase(phrases, current);
        return phrases;
    }

    private static void addPhrase(List<String> phrases, StringBuilder current) {
        String phrase = current.toString().trim();
        if (!phrase.isBlank()) {
            phrases.add(phrase);
        }
        current.setLength(0);
    }

    public static Set<String> wakeBypassPhrases() {
        return provider().wakeBypassPhrases();
    }

    public static Set<String> listenBypassPrefixes() {
        return provider().listenBypassPrefixes();
    }
}
