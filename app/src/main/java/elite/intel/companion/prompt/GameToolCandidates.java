package elite.intel.companion.prompt;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.ai.brain.actions.handlers.query.ConnectionCheckQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.ai.brain.i18n.PromptLocalizations;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.i18n.Language;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Step 1 of game-tool selection, independent of the reduction algorithm: turns the visible game actions
 * of the allowed categories into provider-neutral {@link LlmToolDefinition}s. Reusable by any
 * {@link CompanionActionReducer} implementation - only the narrowing in step 2 is swappable.
 * <p>
 * Sources by category: {@code IntelCommand} registry -> {@code ACTION}, {@code IntelQuery} registry ->
 * {@code QUERY}, user macros -> {@code MACRO}. An action is included only if visible for the current
 * game context ({@code isVisibleForLLM}); a missing localized phrase does <em>not</em> exclude it
 * (native tool-calling selects by name/description/parameters), it only weakens cross-language matching.
 * The legacy fallback ids (general-conversation, ignore-nonsensical, connection-check) are never offered.
 * <p>
 * Per the localized-phrase hard rule (§10.3): when an action has localized training phrases they are
 * embedded into the English {@code description} (the only field the provider sees) to help the model map
 * a non-English commander utterance to the right tool; they are also kept in the neutral
 * {@code localizedTrainingPhrases} field for future dialects.
 */
final class GameToolCandidates {

    /** Legacy-path fallback ids the companion never offers; it has its own speak/nothing_to_do. */
    private static final Set<String> EXCLUDED_IDS = Set.of(
            GeneralConversationQueryCommand.ID,
            ConnectionCheckQueryCommand.ID,
            IgnoreNonsensicalInputCommand.ID);

    /**
     * One selectable game tool.
     *
     * @param id        the action id (matched against the reducer's survivors)
     * @param phraseKey text the word-overlap reducer matches on (localized phrase group, or id when none)
     * @param tool      the rendered, provider-neutral definition
     */
    record Candidate(String id, String phraseKey, LlmToolDefinition tool) {}

    private final Map<String, ? extends IntelAction> commands;
    private final Map<String, ? extends IntelAction> queries;
    private final List<CustomCommandDefinition> macros;
    private final Status status;
    private final Language language;
    /** English exonym of {@link #language} (e.g. "English", "Russian"), naming the example-phrase language. */
    private final String languageName;

    /** Production: pulls the self-describing registries, the live game status, and the configured language. */
    GameToolCandidates() {
        this(CommandRegistry.getInstance().byId(),
                QueryRegistry.getInstance().byId(),
                CustomCommandRegistry.getInstance().getCustomCommands(),
                Status.getInstance(),
                SystemSession.getInstance().getLanguage());
    }

    /** Test seam: inject fixed actions/macros, status, and language (no singletons, no registry scan). */
    GameToolCandidates(Map<String, ? extends IntelAction> commands,
                       Map<String, ? extends IntelAction> queries,
                       List<CustomCommandDefinition> macros,
                       Status status,
                       Language language) {
        this.commands = commands;
        this.queries = queries;
        this.macros = macros;
        this.status = status;
        this.language = language;
        this.languageName = PromptLocalizations.rulesFor(language).languageName();
    }

    /** Visible game tools in the allowed categories, ordered commands, then queries, then macros. */
    List<Candidate> collect(Set<IntelActionCategory> allowed) {
        List<Candidate> result = new ArrayList<>();
        if (allowed.contains(IntelActionCategory.ACTION)) {
            addActions(result, commands);
        }
        if (allowed.contains(IntelActionCategory.QUERY)) {
            addActions(result, queries);
        }
        if (allowed.contains(IntelActionCategory.MACRO)) {
            addMacros(result);
        }
        return result;
    }

    private void addActions(List<Candidate> out, Map<String, ? extends IntelAction> actions) {
        for (IntelAction action : actions.values()) {
            String id = action.id();
            if (EXCLUDED_IDS.contains(id) || !action.isVisibleForLLM(status)) {
                continue;
            }
            boolean hasPhrases = AiActionAliasTextProvider.hasKey(language, id);
            String phraseGroup = hasPhrases ? AiActionAliasTextProvider.getText(language, id) : "";
            // The localized phrase group stays the reducer/reflex matching surface (it must match the
            // commander's own language). The tool DESCRIPTION the model reads carries the action's English
            // purpose plus its English trigger phrases - concrete targets to match, since the model selects
            // from the English schema and translates non-English input to English first. English phrases keep
            // the description identical across languages (one cache prefix), unlike localized phrases.
            String authored = action.llmDescription();
            String base = authored == null ? "" : authored.strip();
            String description = appendEnglishPhrases(base, id);
            out.add(new Candidate(id, hasPhrases ? phraseGroup : id,
                    new LlmToolDefinition(id, description, phraseGroup, action.parameters())));
        }
    }

    private void addMacros(List<Candidate> out) {
        for (CustomCommandDefinition macro : macros) {
            String id = macro.getActionKey();
            if (EXCLUDED_IDS.contains(id)) {
                continue;
            }
            String phraseGroup = macro.getPhrases() == null ? "" : macro.getPhrases().strip();
            String base = macro.getDescription().isBlank()
                    ? "User-defined macro \"" + id + "\"."
                    : macro.getDescription().strip();
            out.add(new Candidate(id, phraseGroup.isBlank() ? id : phraseGroup,
                    new LlmToolDefinition(id, describe(base, phraseGroup), phraseGroup, macro.getParameters())));
        }
    }

    /** Appends the localized example phrases to a base description when present (§10.3 hard rule). */
    private String describe(String base, String phraseGroup) {
        String phrases = examplePhrases(phraseGroup);
        return phrases.isEmpty() ? base : base + " " + phrases;
    }

    /** The localized example-phrases sentence embedded into a tool description (§10.3), or empty when none. */
    private String examplePhrases(String phraseGroup) {
        return phraseGroup.isBlank() ? "" : "Example phrases in " + languageName + ": " + phraseGroup + ".";
    }

    /**
     * Appends the action's English trigger phrases to its tool description, giving the model concrete phrasings
     * to match against. The companion path otherwise shows it only the abstract English purpose, never the
     * phrases (unlike the legacy "action &lt;- phrases" prompt). English on purpose: the schema is English, the
     * model translates non-English input to English before selecting, and an English description stays
     * identical across languages (one cache prefix). Parameter annotations ({@code {key:X}}) are stripped - the
     * JSON schema already carries the parameters.
     */
    private String appendEnglishPhrases(String base, String id) {
        if (!AiActionAliasTextProvider.hasKey(Language.EN, id)) {
            return base;
        }
        String english = stripParamAnnotations(AiActionAliasTextProvider.getText(Language.EN, id));
        if (english.isEmpty()) {
            return base;
        }
        String phrases = "Example phrases: " + english + ".";
        return base.isEmpty() ? phrases : base + " " + phrases;
    }

    /** Strips {@code {key:X}} parameter annotations and tidies the leftover separators/whitespace. */
    private static String stripParamAnnotations(String phrases) {
        return phrases.replaceAll("\\{[^}]*}", "")
                .replaceAll("\\s+,", ",")
                .replaceAll("\\s{2,}", " ")
                .strip();
    }
}
