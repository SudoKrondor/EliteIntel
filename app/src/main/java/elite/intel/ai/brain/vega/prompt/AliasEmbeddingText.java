package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.i18n.AiActionLocalizations;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds natural-language text from parameterized aliases for semantic embedding. */
public final class AliasEmbeddingText {

    private static final Pattern PARAMETER_BLOCK = Pattern.compile("\\{([^{}]+)}");
    private static final String SAMPLE_NUMBER = "20";

    private AliasEmbeddingText() {
    }

    /** Returns embedding-ready aliases with parameter placeholders substituted or removed. */
    public static List<String> phrases(String localizedAliasGroup, List<ActionParameterSpec> parameters) {
        Map<String, String> typeByName = new HashMap<>();
        if (parameters != null) {
            for (ActionParameterSpec parameter : parameters) {
                if (parameter.getName() != null) {
                    typeByName.put(parameter.getName().toLowerCase(Locale.ROOT), parameter.getType());
                }
            }
        }

        List<String> phrases = new ArrayList<>();
        for (String alias : AiActionLocalizations.splitPhraseGroup(localizedAliasGroup)) {
            String phrase = substituteParameters(alias, typeByName);
            if (!phrase.isBlank()) {
                phrases.add(phrase);
            }
        }
        return phrases;
    }

    private static String substituteParameters(String alias, Map<String, String> typeByName) {
        Matcher matcher = PARAMETER_BLOCK.matcher(alias);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            List<String> values = new ArrayList<>();
            for (String token : matcher.group(1).split(",")) {
                String name = token.split(":", 2)[0].trim().toLowerCase(Locale.ROOT);
                if ("number".equals(typeByName.get(name))) {
                    values.add(SAMPLE_NUMBER);
                }
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.join(" ", values)));
        }
        matcher.appendTail(result);
        return result.toString().replaceAll("\\s{2,}", " ").strip();
    }
}
