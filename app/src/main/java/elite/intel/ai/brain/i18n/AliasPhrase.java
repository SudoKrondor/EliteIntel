package elite.intel.ai.brain.i18n;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One training phrase from an action's alias group, split into the words a commander actually says and the
 * arguments the alias itself already pins down.
 * <p>
 * Alias phrases carry their parameters inline as {@code {name:value}} blocks, and the value is written one of
 * two ways. A <em>variable</em> value stands for something only the commander's wording can supply, so it is
 * written as a placeholder letter or a choice - {@code "erhöhe geschwindigkeit um {key:X}"},
 * {@code "{state:true/false}"}. A <em>literal</em> value is baked into the alias itself, because that phrase
 * means exactly one thing: {@code "ziel fsd {key:fsd}"} is always {@code key=fsd}, in every language.
 * <p>
 * The distinction matters because it decides who has to extract the argument. A phrase whose arguments are all
 * literal is fully resolved by the alias author, so matching it verbatim yields both the action and its
 * arguments with nothing left to infer - see {@code ReflexResolver}. A phrase with any variable argument still
 * needs the LLM to read the value out of what the commander said.
 *
 * @param spokenText          the phrase with every {@code {...}} block removed - what the commander says out loud
 * @param literalArguments    argument names to the values the alias pins down, in authoring order; empty when the
 *                            phrase takes no arguments or its arguments are all variable
 * @param hasVariableArgument whether any argument still has to be extracted from the commander's wording
 */
public record AliasPhrase(String spokenText, Map<String, String> literalArguments, boolean hasVariableArgument) {

    /**
     * Matches the {@code {...}} argument blocks inside a phrase, e.g. {@code {key:fsd}} or {@code {lat:X, lon:Y}}.
     */
    private static final Pattern ARGUMENT_BLOCK = Pattern.compile("\\{([^{}]+)}");
    /**
     * A value that stands in for commander wording rather than naming it: a placeholder letter, or a choice.
     */
    private static final Pattern VARIABLE_VALUE = Pattern.compile("[A-Za-z]|.*/.*");
    private static final Pattern VALID_PARAMETER_NAME = Pattern.compile("[A-Za-z0-9_]+");

    public AliasPhrase {
        literalArguments = Map.copyOf(literalArguments);
    }

    /**
     * Parses one alias phrase. A malformed or unnamed argument counts as variable, so anything this parser does
     * not fully understand keeps taking the LLM path rather than being executed on a guess.
     */
    public static AliasPhrase parse(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return new AliasPhrase("", Map.of(), false);
        }
        Map<String, String> literals = new LinkedHashMap<>();
        boolean variable = false;
        Matcher blocks = ARGUMENT_BLOCK.matcher(phrase);
        while (blocks.find()) {
            for (String token : splitArguments(blocks.group(1))) {
                String[] parts = token.split(":", 2);
                String name = parts[0].trim();
                String value = parts.length < 2 ? "" : parts[1].trim();
                if (!VALID_PARAMETER_NAME.matcher(name).matches()
                        || value.isEmpty()
                        || VARIABLE_VALUE.matcher(value).matches()) {
                    variable = true;
                } else {
                    literals.put(name, value);
                }
            }
        }
        return new AliasPhrase(stripArgumentBlocks(phrase), literals, variable);
    }

    /**
     * Splits one block's comma-separated {@code name:value} tokens, e.g. {@code "lat:X, lon:Y"}.
     */
    private static List<String> splitArguments(String block) {
        return java.util.Arrays.stream(block.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    /**
     * Removes every argument block and collapses the whitespace they leave behind.
     */
    private static String stripArgumentBlocks(String phrase) {
        return ARGUMENT_BLOCK.matcher(phrase).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }
}
