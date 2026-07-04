package elite.intel.companion.prompt;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.i18n.AiActionLocalizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a command's localized alias group into the phrase list the semantic matcher should embed. The raw
 * aliases carry {@code {name:hint}} parameter placeholders (e.g. {@code сбавь ход на {key:X}}); embedding those
 * literally hurts matching, because {@code {key:X}} is noise text the query never contains. This owner replaces
 * each placeholder with a representative value so the surface reads like a real utterance:
 * <ul>
 *   <li><b>number</b> params -&gt; a sample number ({@code сбавь ход на 20}); measured to match a real
 *       "сбавь скорость на двадцать" far better than either the annotated or the stripped form.</li>
 *   <li><b>everything else</b> (enum/string/boolean) -&gt; stripped: the value is already carried by the
 *       phrase's noun ("цель двигатели") or verb ("включи/выключи"), so injecting a code would only add noise.</li>
 * </ul>
 * The authored alias bundles and the parameter annotations are untouched; only the text fed to the embedder is
 * derived here. Single source of truth so the reducer and the training-phrase probe embed the same surface.
 */
public final class AliasMatchSurface {

    private static final Pattern PLACEHOLDER_BLOCK = Pattern.compile("\\{([^{}]+)}");
    private static final String SAMPLE_NUMBER = "20";

    private AliasMatchSurface() {
    }

    /**
     * The embedding-ready phrases for an alias group: placeholders substituted by type, blanks dropped.
     *
     * @param phraseGroup the localized alias group (comma-separated, possibly with {@code {name:hint}} blocks)
     * @param params      the action's parameter schema, used to type each placeholder (may be null/empty)
     */
    public static List<String> phrases(String phraseGroup, List<ActionParameterSpec> params) {
        Map<String, String> typeByName = new HashMap<>();
        if (params != null) {
            for (ActionParameterSpec spec : params) {
                if (spec.getName() != null) {
                    typeByName.put(spec.getName().toLowerCase(Locale.ROOT), spec.getType());
                }
            }
        }
        List<String> out = new ArrayList<>();
        for (String alias : AiActionLocalizations.splitPhraseGroup(phraseGroup)) {
            String substituted = substitute(alias, typeByName);
            if (!substituted.isBlank()) {
                out.add(substituted);
            }
        }
        return out;
    }

    /** Replaces each {@code {name:hint}} block with its params' representative values (numbers) or nothing. */
    private static String substitute(String alias, Map<String, String> typeByName) {
        Matcher matcher = PLACEHOLDER_BLOCK.matcher(alias);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            List<String> values = new ArrayList<>();
            for (String token : matcher.group(1).split(",")) {
                String name = token.split(":", 2)[0].trim().toLowerCase(Locale.ROOT);
                if ("number".equals(typeByName.get(name))) {
                    values.add(SAMPLE_NUMBER);
                }
                // enum/string/boolean: strip - the phrase's own noun/verb already carries the meaning.
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.join(" ", values)));
        }
        matcher.appendTail(result);
        return result.toString().replaceAll("\\s{2,}", " ").strip();
    }
}
