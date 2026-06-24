package elite.intel.ai.brain.i18n;

/**
 * Per-language prompt fragments injected into the command-classification prompt.
 * <p>
 * One implementation per language lives in this package — parallel to
 * {@link InputNormalizerProvider} and {@link AiActionAliasProvider}. Edit only
 * your own language file; no merge conflicts with other localizers.
 * <p>
 * All return values are plain strings injected verbatim into the LLM system prompt,
 * so keep them concise and example-style (not prose).
 */
public interface PromptLanguageRules {

    /**
     * Full English display name, e.g. {@code "Russian"}, {@code "German"}.
     */
    String languageName();

    /**
     * Comma-separated example query-starter words for the CLASSIFICATION section.
     * Example (English): {@code "what, where, how, which, why, how much, how many"}
     */
    String queryStarterExamples();

    /**
     * Slash-separated command verb examples for the VERB INTENT section.
     * Example (English): {@code "show / open / find / navigate / deploy / retract"}
     */
    String commandVerbExamples();

    /**
     * Slash-separated query phrase examples for the VERB INTENT section.
     * Example (English): {@code "where / tell me / how much / how many / what is"}
     */
    String queryPhraseExamples();

    /**
     * Language-specific colloquial trigger phrases for the DISAMBIGUATION section,
     * appended after the universal game-logic rules.
     * <p>
     * Return a multi-line string (no leading indent — appended verbatim to the prompt),
     * or {@code null} if no language-specific hints are needed yet.
     * <p>
     * Each line should follow the pattern:
     * {@code - "phrase1" / "phrase2" → action_name}
     * <p>
     * These are colloquial expressions players use — not 1-to-1 translations of the
     * English phrases, but natural equivalents a speaker of this language would say.
     */
    default String disambiguationHints() {
        return null;
    }
}
