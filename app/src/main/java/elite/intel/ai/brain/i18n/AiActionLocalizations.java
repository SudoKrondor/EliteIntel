package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.AiActionsMap;
import elite.intel.session.SystemSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AiActionLocalizations {

    private AiActionLocalizations() {
    }

    public static List<String> phrasesForAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return List.of();
        }
        if (AiActionAliasTextProvider.hasKey(SystemSession.getInstance().getLanguage(), actionId)) {
            return splitPhraseGroup(AiActionAliasTextProvider.getText(
                            SystemSession.getInstance().getLanguage(), actionId)).stream()
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        // Custom commands are not resource-bundle entries, so retain the runtime map as their fallback source.
        Map<String, String> fullMap = AiActionsMap.getInstance().actionMap(true);
        return fullMap.entrySet().stream()
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
}
