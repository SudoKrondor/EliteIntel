package elite.intel.companion.prompt;

import elite.intel.companion.model.ThoughtSource;

/** Replaceable source of source-specific static rules, kept injectable for prompt-composer tests. */
public interface SystemPromptText {

    /**
     * The static, slowly-changing instruction block for a thought of the given source. The text is in
     * English by design; only the language rule names the commander's language for spoken output.
     *
     * @param source whether the turn was started by the commander or a game event (rules differ)
     * @return the assembled static instruction text, ending with a newline
     */
    String staticRules(ThoughtSource source);
}
